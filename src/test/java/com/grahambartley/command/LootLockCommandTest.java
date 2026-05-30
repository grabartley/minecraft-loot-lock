package com.grahambartley.command;

import com.grahambartley.data.FilterMode;
import com.grahambartley.data.RejectedItemAction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LootLockCommandTest {

    @Test
    void modeTokenMapsEnumToCommandToken() {
        assertEquals("denylist", LootLockCommand.modeToken(FilterMode.DENYLIST));
        assertEquals("allowlist", LootLockCommand.modeToken(FilterMode.ALLOWLIST));
    }

    @Test
    void actionTokenMapsEnumToCommandToken() {
        assertEquals("leave", LootLockCommand.actionToken(RejectedItemAction.LEAVE_ON_GROUND));
        assertEquals("delete", LootLockCommand.actionToken(RejectedItemAction.DELETE));
    }
}
