package org.jfrog.bamboo.bintray.client;

/**
 * Response object to represent MavenSync API response
 *
 * @author Aviad Shikloshi
 */
public class MavenSyncResponse {

    private String status;
    private String messages;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessages() {
        return messages;
    }

    public void setMessages(String messages) {
        this.messages = messages;
    }

}
