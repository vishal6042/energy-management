package com.dems.orchestrator.agent.action;

import com.dems.orchestrator.client.BackendApi;
import com.dems.orchestrator.client.dto.DeviceDto;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Drafts and executes the HITL action tools by binding each to a validated backend call. */
@Component
public class ToolDispatcher {

    private final BackendApi backend;
    private final DeviceResolver resolver;

    public ToolDispatcher(BackendApi backend, DeviceResolver resolver) {
        this.backend = backend;
        this.resolver = resolver;
    }

    /** Human-readable impact summary for the HITL confirmation card. */
    public String describeImpact(String name, Map<String, Object> args) {
        return switch (name) {
            case "set_device_status" -> {
                String device = arg(args, "device");
                String status = lower(arg(args, "status"));
                DeviceDto d = resolver.resolve(device).orElse(null);
                String label = d == null ? device : d.name() + " (" + d.id() + ")";
                if ("online".equals(status)) {
                    String kw = d == null ? "its rated" : round(d.nominalPowerW() / 1000.0, 2) + " kW";
                    yield "Turn ON " + label + ". It will draw ~" + kw
                            + " and resume contributing to usage and savings from the current period.";
                }
                yield "Turn OFF " + label + ". It will stop drawing power and contribute nothing while offline.";
            }
            case "apply_algorithm" -> {
                String device = arg(args, "device");
                String algo = lower(arg(args, "algorithm"));
                DeviceDto d = resolver.resolve(device).orElse(null);
                String label = d == null ? device : d.name() + " (" + d.id() + ")";
                if ("none".equals(algo)) {
                    yield "Clear the algorithm on " + label + "; it will stop generating savings.";
                }
                yield "Apply the '" + algo + "' algorithm to " + label
                        + "; it will begin generating energy/cost savings.";
            }
            default -> "Execute " + name + " with " + args;
        };
    }

    /** Execute a confirmed action and return a friendly result message. */
    public String executeAction(String name, Map<String, Object> args) {
        return switch (name) {
            case "set_device_status" -> {
                DeviceDto d = requireDevice(arg(args, "device"));
                String status = lower(arg(args, "status"));
                DeviceDto updated = backend.setDeviceStatus(d.id(), Map.of("status", status));
                yield updated.name() + " is now " + updated.status()
                        + " (power " + round(updated.powerW() / 1000.0, 2) + " kW).";
            }
            case "apply_algorithm" -> {
                DeviceDto d = requireDevice(arg(args, "device"));
                String algo = lower(arg(args, "algorithm"));
                DeviceDto updated = backend.applyAlgorithm(d.id(), Map.of("algorithm", algo));
                yield "none".equals(algo)
                        ? "Cleared the algorithm on " + updated.name() + "."
                        : "Applied the '" + algo + "' algorithm to " + updated.name() + ".";
            }
            default -> "Unsupported action: " + name;
        };
    }

    private DeviceDto requireDevice(String idOrName) {
        return resolver.resolve(idOrName)
                .orElseThrow(() -> new IllegalArgumentException("No device matched '" + idOrName + "'"));
    }

    private static String arg(Map<String, Object> args, String key) {
        Object v = args == null ? null : args.get(key);
        if (v == null) {
            return null;
        }
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    private static String lower(String s) {
        return s == null ? null : s.toLowerCase();
    }

    private static double round(double v, int dp) {
        double f = Math.pow(10, dp);
        return Math.round(v * f) / f;
    }
}
