package pl.ecommerce.commons.command;

public interface CommandHandler<T extends Command> {
	void handle(T command);
}