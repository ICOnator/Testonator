package io.iconator.testrpcj;

import org.web3j.abi.datatypes.Type;

import java.util.List;

public class Event {
    private final List<Type> values;
    private final String name;
    private final String signature;
    private final String topic;
    public Event(List<Type> values, String name, String signature, String topic) {
        this.values = values;
        this.name = name;
        this.signature = signature;
        this.topic = topic;
    }
    public List<Type> values() {
        return values;
    }
    public String name() {
        return name;
    }
    public String signature() {
        return signature;
    }
    public String topic() {
        return topic;
    }
}
