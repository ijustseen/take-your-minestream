package takeyourminestream.ijustseen.messages;

/**
 * Фабрика для создания и инициализации системы сообщений
 */
public class MessageSystemFactory {
    
    /**
     * Создает и инициализирует все компоненты системы сообщений
     * @return настроенный MessageSpawner
     */
    public static MessageSpawner createMessageSystem() {
        MessageQueue messageQueue = new MessageQueue();
        MessageParticleManager particleManager = new MessageParticleManager();
        MessageLifecycleManager lifecycleManager = new MessageLifecycleManager(particleManager);
        MessageSpawner messageSpawner = new MessageSpawner(messageQueue, lifecycleManager);
        new MessageRenderer(lifecycleManager, particleManager);
        new MessageHudRenderer(lifecycleManager);
        return messageSpawner;
    }
} 