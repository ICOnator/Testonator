package io.iconator.testrpcj;

import org.web3j.abi.datatypes.Type;

import java.util.List;

public class Event {
    private final Contract contract;
    private final List<Type> values;
    private final String name;
    private final String signature;
    private final String topic;
    public Event(Contract contract, List<Type> values, String name, String signature, String topic) {
        this.contract = contract;
        this.values = values;
        this.name = name;
        this.signature = signature;
        this.topic = topic;
    }
    public Contract contract() {return contract;}
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
