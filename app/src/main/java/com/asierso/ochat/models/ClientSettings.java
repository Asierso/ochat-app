package com.asierso.ochat.models;

import java.util.Objects;

public class ClientSettings {
    private String ip;
    private int port = 11434;
    private String model;
    private boolean ssl;
    private boolean useDescriptions = true;
    private boolean optimizeModels = false;

    public boolean isUseDescriptions() {
        return useDescriptions;
    }

    public void setUseDescriptions(boolean useDescriptions) {
        this.useDescriptions = useDescriptions;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public boolean isSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    public boolean isOptimizeModels() {
        return optimizeModels;
    }

    public void setOptimizeModels(boolean optimizeModels) {
        this.optimizeModels = optimizeModels;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientSettings that = (ClientSettings) o;
        return port == that.port && ssl == that.ssl && Objects.equals(ip, that.ip) && Objects.equals(model, that.model);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, port, model, ssl);
    }
}
