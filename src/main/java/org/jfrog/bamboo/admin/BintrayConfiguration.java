package org.jfrog.bamboo.admin;

import java.io.Serializable;

/**
 * Bintray admin configuration
 *
 * @author Aviad Shikloshi
 */
public class BintrayConfiguration implements Serializable {


    private String bintrayUsername;
    private String bintrayApiKey;
    private String sonatypeOssUsername;
    private String sonatypeOssPassword;

    public BintrayConfiguration() {}

    public BintrayConfiguration(String bintrayUsername, String bintrayApiKey, String sonatypeOssUsername, String sonatypeOssPassword) {
        this.bintrayUsername = bintrayUsername;
        this.bintrayApiKey = bintrayApiKey;
        this.sonatypeOssUsername = sonatypeOssUsername;
        this.sonatypeOssPassword = sonatypeOssPassword;
    }

    public String getBintrayUsername() {
        return bintrayUsername;
    }

    public String getBintrayApiKey() {
        return bintrayApiKey;
    }

    public String getSonatypeOssUsername() {
        return sonatypeOssUsername;
    }

    public String getSonatypeOssPassword() {
        return sonatypeOssPassword;
    }

    public void setBintrayApiKey(String bintrayApiKey) {
        this.bintrayApiKey = bintrayApiKey;
    }

    public void setSonatypeOssPassword(String sonatypeOssPassword) {
        this.sonatypeOssPassword = sonatypeOssPassword;
    }

    public void setBintrayUsername(String bintrayUsername) {
        this.bintrayUsername = bintrayUsername;
    }

    public void setSonatypeOssUsername(String sonatypeOssUsername) {
        this.sonatypeOssUsername = sonatypeOssUsername;
    }
}
