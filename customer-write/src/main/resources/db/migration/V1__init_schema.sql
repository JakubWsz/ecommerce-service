CREATE TABLE IF NOT EXISTS event_store
(
    id              BIGSERIAL PRIMARY KEY,
    event_id        UUID                     NOT NULL,
    aggregate_id    UUID                     NOT NULL,
    aggregate_type  VARCHAR(100)             NOT NULL,
    event_type      VARCHAR(100)             NOT NULL,
    version         INT                      NOT NULL,
    event_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    event_data      JSONB                    NOT NULL,
    trace_id        VARCHAR(100),
    span_id         VARCHAR(100),
    deleted         BOOLEAN                  DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_event_store_aggregate_id ON event_store (aggregate_id);

CREATE INDEX idx_event_store_event_type ON event_store (event_type);

CREATE INDEX idx_event_store_trace_id ON event_store (trace_id);

CREATE UNIQUE INDEX idx_event_store_aggregate_version
    ON event_store (aggregate_id, version, deleted)
    WHERE deleted = false;

CREATE INDEX idx_event_store_timestamp
    ON event_store (event_timestamp);

CREATE TABLE IF NOT EXISTS customer_snapshots
(
    id            BIGSERIAL PRIMARY KEY,
    customer_id   UUID  NOT NULL,
    version       INT   NOT NULL,
    snapshot_data JSONB NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_customer_snapshots UNIQUE (customer_id, version)
);

CREATE INDEX idx_customer_snapshots_customer_id
    ON customer_snapshots (customer_id);

CREATE OR REPLACE VIEW customer_email_view AS
SELECT DISTINCT ON (aggregate_id) aggregate_id                 AS customer_id,
                                  (event_data ->> 'email')     AS email,
                                  (event_data ->> 'firstName') AS first_name,
                                  (event_data ->> 'lastName')  AS last_name,
                                  MAX(version)                 AS latest_version
FROM event_store
WHERE aggregate_type = 'Customer'
  AND event_type IN ('CustomerRegisteredEvent', 'CustomerEmailChangedEvent')
  AND deleted = false
GROUP BY aggregate_id, event_data ->> 'email', event_data ->> 'firstName', event_data ->> 'lastName'
ORDER BY aggregate_id, latest_version DESC;

CREATE OR REPLACE VIEW customer_status_view AS
SELECT DISTINCT ON (aggregate_id) aggregate_id    AS customer_id,
                                  CASE
                                      WHEN event_type = 'CustomerDeletedEvent' THEN 'DELETED'
                                      WHEN event_type = 'CustomerDeactivatedEvent' THEN 'INACTIVE'
                                      WHEN event_type = 'CustomerReactivatedEvent' THEN 'ACTIVE'
                                      WHEN event_type = 'CustomerRegisteredEvent' THEN 'ACTIVE'
                                      ELSE 'UNKNOWN'
                                      END         AS status,
                                  event_timestamp AS last_status_change,
                                  version
FROM event_store
WHERE aggregate_type = 'Customer'
  AND event_type IN (
                     'CustomerRegisteredEvent',
                     'CustomerDeactivatedEvent',
                     'CustomerReactivatedEvent',
                     'CustomerDeletedEvent'
    )
  AND deleted = false
ORDER BY aggregate_id, version DESC;

CREATE OR REPLACE VIEW customer_verification_view AS
SELECT customer_id,
       BOOL_OR(verification_type = 'email' AND is_verified) AS email_verified,
       BOOL_OR(verification_type = 'phone' AND is_verified) AS phone_verified
FROM (SELECT aggregate_id AS customer_id,
             CASE
                 WHEN event_type = 'CustomerEmailVerifiedEvent' THEN 'email'
                 WHEN event_type = 'CustomerPhoneVerifiedEvent' THEN 'phone'
                 ELSE 'unknown'
                 END      AS verification_type,
             true         AS is_verified,
             version
      FROM event_store
      WHERE aggregate_type = 'Customer'
        AND event_type IN ('CustomerEmailVerifiedEvent', 'CustomerPhoneVerifiedEvent')
        AND deleted = false) AS verifications
GROUP BY customer_id;

CREATE OR REPLACE VIEW customer_address_view AS
SELECT e1.aggregate_id                          AS customer_id,
       (e1.event_data ->> 'addressId')::UUID    AS address_id,
       e1.event_data ->> 'addressType'          AS address_type,
       e1.event_data ->> 'street'               AS street,
       e1.event_data ->> 'buildingNumber'       AS building_number,
       e1.event_data ->> 'apartmentNumber'      AS apartment_number,
       e1.event_data ->> 'city'                 AS city,
       e1.event_data ->> 'postalCode'           AS postal_code,
       e1.event_data ->> 'country'              AS country,
       e1.event_data ->> 'voivodeship'          AS voivodeship,
       (e1.event_data ->> 'isDefault')::BOOLEAN AS is_default,
       e1.version
FROM event_store e1
WHERE e1.aggregate_type = 'Customer'
  AND e1.event_type IN ('CustomerAddressAddedEvent', 'CustomerAddressUpdatedEvent')
  AND e1.deleted = false
  AND NOT EXISTS (SELECT 1
                  FROM event_store e2
                  WHERE e2.aggregate_type = 'Customer'
                    AND e2.event_type = 'CustomerAddressRemovedEvent'
                    AND e2.aggregate_id = e1.aggregate_id
                    AND (e2.event_data ->> 'addressId')::UUID = (e1.event_data ->> 'addressId')::UUID
                    AND e2.version > e1.version
                    AND e2.deleted = false)
ORDER BY customer_id, address_id, version DESC;

CREATE OR REPLACE FUNCTION get_customer_state(p_customer_id UUID)
    RETURNS JSONB AS
$$
DECLARE
    result       JSONB := '{}'::JSONB;
    event_record RECORD;
BEGIN
    result := jsonb_build_object(
            'id', p_customer_id,
            'status', 'UNKNOWN',
            'email_verified', false,
            'phone_verified', false,
            'addresses', '[]'::JSONB,
            'preferences', '{}'::JSONB,
            'metadata', '{}'::JSONB,
            'version', 0
              );

    FOR event_record IN (SELECT event_type,
                                event_data,
                                version
                         FROM event_store
                         WHERE aggregate_id = p_customer_id
                           AND deleted = false
                         ORDER BY version ASC)
        LOOP
            CASE event_record.event_type
                WHEN 'CustomerRegisteredEvent'
                    THEN result := jsonb_set(result, '{email}', event_record.event_data -> 'email');
                         result := jsonb_set(result, '{firstName}', event_record.event_data -> 'firstName');
                         result := jsonb_set(result, '{lastName}', event_record.event_data -> 'lastName');
                         result := jsonb_set(result, '{phoneNumber}', event_record.event_data -> 'phoneNumber');
                         result := jsonb_set(result, '{status}', '"ACTIVE"');
                         result := jsonb_set(result, '{version}', to_jsonb(event_record.version));

                WHEN 'CustomerUpdatedEvent'
                    THEN IF event_record.event_data -> 'changes' ->> 'firstName' IS NOT NULL THEN
                        result := jsonb_set(result, '{firstName}', event_record.event_data -> 'changes' -> 'firstName');
                         END IF;

                         IF event_record.event_data -> 'changes' ->> 'lastName' IS NOT NULL THEN
                             result := jsonb_set(result, '{lastName}',
                                                 event_record.event_data -> 'changes' -> 'lastName');
                         END IF;

                         IF event_record.event_data -> 'changes' ->> 'phoneNumber' IS NOT NULL THEN
                             result := jsonb_set(result, '{phoneNumber}',
                                                 event_record.event_data -> 'changes' -> 'phoneNumber');
                             result := jsonb_set(result, '{phone_verified}', 'false');
                         END IF;

                         result := jsonb_set(result, '{version}', to_jsonb(event_record.version));

                WHEN 'CustomerEmailChangedEvent'
                    THEN result := jsonb_set(result, '{email}', event_record.event_data -> 'newEmail');
                         result := jsonb_set(result, '{email_verified}', 'false');
                         result := jsonb_set(result, '{version}', to_jsonb(event_record.version));

                WHEN 'CustomerEmailVerifiedEvent' THEN result := jsonb_set(result, '{email_verified}', 'true');
                                                       result :=
                                                               jsonb_set(result, '{version}', to_jsonb(event_record.version));

                WHEN 'CustomerPhoneVerifiedEvent' THEN result := jsonb_set(result, '{phone_verified}', 'true');
                                                       result :=
                                                               jsonb_set(result, '{version}', to_jsonb(event_record.version));

                WHEN 'CustomerDeactivatedEvent' THEN result := jsonb_set(result, '{status}', '"INACTIVE"');
                                                     result := jsonb_set(result, '{deactivation_reason}',
                                                                         event_record.event_data -> 'reason');
                                                     result :=
                                                             jsonb_set(result, '{version}', to_jsonb(event_record.version));

                WHEN 'CustomerReactivatedEvent' THEN result := jsonb_set(result, '{status}', '"ACTIVE"');
                                                     result :=
                                                             jsonb_set(result, '{version}', to_jsonb(event_record.version));

                WHEN 'CustomerDeletedEvent' THEN result := jsonb_set(result, '{status}', '"DELETED"');
                                                 result :=
                                                         jsonb_set(result, '{version}', to_jsonb(event_record.version));

                WHEN 'CustomerAddressAddedEvent' THEN DECLARE
                    address   JSONB;
                    addresses JSONB;
                BEGIN
                    address := jsonb_build_object(
                            'id', event_record.event_data -> 'addressId',
                            'type', event_record.event_data -> 'addressType',
                            'street', event_record.event_data -> 'street',
                            'buildingNumber', event_record.event_data -> 'buildingNumber',
                            'apartmentNumber', event_record.event_data -> 'apartmentNumber',
                            'city', event_record.event_data -> 'city',
                            'postalCode', event_record.event_data -> 'postalCode',
                            'country', event_record.event_data -> 'country',
                            'state', event_record.event_data -> 'state',
                            'isDefault', event_record.event_data -> 'isDefault'
                               );

                    addresses := result -> 'addresses';
                    addresses := addresses || address;
                    result := jsonb_set(result, '{addresses}', addresses);
                    result := jsonb_set(result, '{version}', to_jsonb(event_record.version));
                END;

                WHEN 'CustomerAddressUpdatedEvent' THEN DECLARE
                    address_id        UUID;
                    addresses         JSONB;
                    updated_addresses JSONB := '[]'::JSONB;
                    address           JSONB;
                BEGIN
                    address_id := (event_record.event_data ->> 'addressId')::UUID;
                    addresses := result -> 'addresses';

                    FOR i IN 0..jsonb_array_length(addresses) - 1
                        LOOP
                            address := addresses -> i;
                            IF (address ->> 'id')::UUID = address_id THEN
                                address := jsonb_set(address, '{street}', event_record.event_data -> 'street');
                                address := jsonb_set(address, '{buildingNumber}',
                                                     event_record.event_data -> 'buildingNumber');
                                address := jsonb_set(address, '{apartmentNumber}',
                                                     event_record.event_data -> 'apartmentNumber');
                                address := jsonb_set(address, '{city}', event_record.event_data -> 'city');
                                address := jsonb_set(address, '{postalCode}', event_record.event_data -> 'postalCode');
                                address := jsonb_set(address, '{country}', event_record.event_data -> 'country');
                                address :=
                                        jsonb_set(address, '{voivodeship}', event_record.event_data -> 'voivodeship');
                                address := jsonb_set(address, '{isDefault}', event_record.event_data -> 'isDefault');
                            END IF;
                            updated_addresses := updated_addresses || address;
                        END LOOP;

                    result := jsonb_set(result, '{addresses}', updated_addresses);
                    result := jsonb_set(result, '{version}', to_jsonb(event_record.version));
                END;

                WHEN 'CustomerAddressRemovedEvent' THEN DECLARE
                    address_id        UUID;
                    addresses         JSONB;
                    updated_addresses JSONB := '[]'::JSONB;
                    address           JSONB;
                BEGIN
                    address_id := (event_record.event_data ->> 'addressId')::UUID;
                    addresses := result -> 'addresses';

                    FOR i IN 0..jsonb_array_length(addresses) - 1
                        LOOP
                            address := addresses -> i;
                            IF (address ->> 'id')::UUID != address_id THEN
                                updated_addresses := updated_addresses || address;
                            END IF;
                        END LOOP;

                    result := jsonb_set(result, '{addresses}', updated_addresses);
                    result := jsonb_set(result, '{version}', to_jsonb(event_record.version));
                END;

                WHEN 'CustomerPreferencesUpdatedEvent'
                    THEN result := jsonb_set(result, '{preferences}', event_record.event_data -> 'preferences');
                         result := jsonb_set(result, '{version}', to_jsonb(event_record.version));

                ELSE RAISE NOTICE 'Unknown event type: %', event_record.event_type;
                END CASE;
        END LOOP;

    RETURN result;
END;
$$ LANGUAGE plpgsql;