package pl.ecommerce;

import software.constructs.Construct;
import org.cdk8s.plus28.k8s.*;

import java.util.List;
import java.util.Map;

public class MicroserviceConstruct extends Construct {
    private final int containerPort;
    private final int servicePort;
    private static final String DEPLOY_SUFFIX  = "Deploy";
    private static final String SERVICE_SUFFIX = "Svc";
    private static final String APP_LABEL      = "app";

    public MicroserviceConstruct(final Construct scope,
                                 final String id,
                                 final String svcName,
                                 final String image,
                                 final int replicas,
                                 final int containerPort,
                                 final int servicePort) {
        super(scope, id);
        this.containerPort = containerPort;
        this.servicePort   = servicePort;

        new KubeDeployment(this,
                id + DEPLOY_SUFFIX,
                KubeDeploymentProps.builder()
                        .metadata(ObjectMeta.builder().name(svcName).build())
                        .spec(buildDeploymentSpec(svcName, image, replicas))
                        .build()
        );

        new KubeService(this,
                id + SERVICE_SUFFIX,
                KubeServiceProps.builder()
                        .metadata(ObjectMeta.builder().name(svcName).build())
                        .spec(buildServiceSpec(svcName))
                        .build()
        );
    }

    private DeploymentSpec buildDeploymentSpec(String svcName, String image, int replicas) {
        return DeploymentSpec.builder()
                .replicas(replicas)
                .selector(buildSelector(svcName))
                .template(buildPodTemplateSpec(svcName, image))
                .build();
    }

    private PodTemplateSpec buildPodTemplateSpec(String svcName, String image) {
        return PodTemplateSpec.builder()
                .metadata(ObjectMeta.builder()
                        .labels(Map.of(APP_LABEL, svcName))
                        .build())
                .spec(buildPodSpec(svcName, image))
                .build();
    }

    private PodSpec buildPodSpec(String svcName, String image) {
        return PodSpec.builder()
                .containers(buildContainers(svcName, image))
                .build();
    }

    private List<Container> buildContainers(String svcName, String image) {
        return List.of(
                Container.builder()
                        .name(svcName)
                        .image(image)
                        .ports(getContainerPorts())
                        .build()
        );
    }

    private List<ContainerPort> getContainerPorts() {
        return List.of(
                ContainerPort.builder()
                        .containerPort(containerPort)
                        .build()
        );
    }

    private LabelSelector buildSelector(String svcName) {
        return LabelSelector.builder()
                .matchLabels(Map.of(APP_LABEL, svcName))
                .build();
    }

    private ServiceSpec buildServiceSpec(String svcName) {
        return ServiceSpec.builder()
                .selector(Map.of(APP_LABEL, svcName))
                .ports(List.of(buildServicePort()))
                .build();
    }

    private ServicePort buildServicePort() {
        return ServicePort.builder()
                .port(servicePort)
                .targetPort(IntOrString.fromNumber(containerPort))
                .build();
    }
}
