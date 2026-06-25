package com.dems.orchestrator.assistant;

import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * RBAC seam for the HITL "Validation" step. The blueprint requires actions to be
 * checked against the logged-in user's permissions. There is no auth in the app
 * yet, so this is a permissive stub — the real check plugs in here later.
 */
@Service
public class PermissionService {

    public boolean canExecute(String tool, Map<String, Object> args) {
        // TODO: map the authenticated user's RBAC roles to allowed actions.
        return true;
    }
}
