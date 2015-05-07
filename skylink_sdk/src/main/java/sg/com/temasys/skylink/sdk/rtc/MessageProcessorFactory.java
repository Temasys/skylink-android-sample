package sg.com.temasys.skylink.sdk.rtc;

/**
 * Purpose of this class is to create MessageProcessors depending on message type received
 */
class MessageProcessorFactory {

    private static final String IN_ROOM = "inRoom";
    private static final String ENTER = "enter";
    private static final String WELCOME = "welcome";
    private static final String RESTART = "restart";
    private static final String ANSWER = "answer";
    private static final String OFFER = "offer";
    private static final String GROUP = "group";
    private static final String CHAT = "chat";
    private static final String BYE = "bye";
    private static final String CANDIDATE = "candidate";
    private static final String PING = "ping";
    private static final String REDIRECT = "redirect";
    private static final String PRIVATE = "private";
    private static final String PUBLIC = "public";
    private static final String UPDATE_USER_EVENT = "updateUserEvent";
    private static final String ROOM_LOCK_EVENT = "roomLockEvent";
    private static final String MUTE_AUDIO_EVENT = "muteAudioEvent";
    private static final String MUTE_VIDEO_EVENT = "muteVideoEvent";

    /**
     * Returns a message type depending on the messageType
     *
     * @param messageType Signaling message
     * @return MessageProcessor or null if the message type is not supported
     */
    public MessageProcessor getMessageProcessor(String messageType) {

        MessageProcessor messageProcessor = getHandshakeMessageProcessor(messageType);

        if (messageProcessor == null) {
            messageProcessor = getOtherMessageProcessor(messageType);
        }

        return messageProcessor;
    }

    /**
     * Returns the appropriate handshake message processor
     *
     * @param messageType Signaling message
     * @return Message processor or null for unsupported message types
     */
    private MessageProcessor getHandshakeMessageProcessor(String messageType) {

        MessageProcessor messageProcessor = null;

        // TODO: Use switch or use a map

        if (IN_ROOM.equalsIgnoreCase(messageType)) {
            messageProcessor = new InRoomMessageProcessor();
        } else if (ENTER.equalsIgnoreCase(messageType)) {
            messageProcessor = new EnterMessageProcessor();
        } else if (WELCOME.equalsIgnoreCase(messageType)) {
            //TODO: handle welcome message types
        } else if (RESTART.equalsIgnoreCase(messageType)) {
            //TODO: handle welcome restart types
        } else if (ANSWER.equalsIgnoreCase(messageType) ||
                OFFER.equalsIgnoreCase(messageType)) {
            messageProcessor = new OfferAnswerMessageProcessor();
        } else if (BYE.equalsIgnoreCase(messageType)) {
            //TODO: handle bye
        } else if (CANDIDATE.equalsIgnoreCase(messageType)) {
            //TODO: handle candidate
        } else if (PING.equalsIgnoreCase(messageType)) {
            messageProcessor = new PingMessageProcessor();
        } else if (REDIRECT.equalsIgnoreCase(messageType)) {
            //TODO: handle ping
        }

        return messageProcessor;
    }

    /**
     * Returns the appropriate message processor for the message
     *
     * @param messageType Signaling message
     * @return Message processor or null for unsupported message types
     */
    private MessageProcessor getOtherMessageProcessor(String messageType) {

        MessageProcessor messageProcessor = null;

        if (CHAT.equalsIgnoreCase(messageType)) {
            //TODO: handle chat
        } else if (UPDATE_USER_EVENT.equalsIgnoreCase(messageType)) {
            //TODO: handle ping
        } else if (GROUP.equalsIgnoreCase(messageType)) {
            //TODO: handle ping
        } else if (PRIVATE.equalsIgnoreCase(messageType) ||
                PUBLIC.equalsIgnoreCase(messageType)) {
            //TODO: handle public and private
        } else if (ROOM_LOCK_EVENT.equalsIgnoreCase(messageType)) {
            //TODO: handle ping
        } else if (MUTE_AUDIO_EVENT.equalsIgnoreCase(messageType)) {
            messageProcessor = new MuteAudioMessageProcessor();
        } else if (MUTE_VIDEO_EVENT.equalsIgnoreCase(messageType)) {
            //TODO: handle muteAudioEvent
        }

        return messageProcessor;
    }
}
