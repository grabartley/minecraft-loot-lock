package com.grahambartley.command;

import com.grahambartley.data.FilterMode;
import com.grahambartley.data.RejectedItemAction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LootLockCommandTest {

    @Test
    void parseModeAcceptsExpectedTokensCaseInsensitive() {
        assertEquals(FilterMode.DENYLIST, LootLockCommand.parseMode("denylist"));
        assertEquals(FilterMode.ALLOWLIST, LootLockCommand.parseMode("ALLOWLIST"));
    }

    @Test
    void parseModeReturnsNullForUnknownToken() {
        assertNull(LootLockCommand.parseMode("not-a-mode"));
        assertNull(LootLockCommand.parseMode(null));
    }

    @Test
    void parseActionAcceptsExpectedTokensCaseInsensitive() {
        assertEquals(RejectedItemAction.LEAVE_ON_GROUND, LootLockCommand.parseAction("leave"));
        assertEquals(RejectedItemAction.DELETE, LootLockCommand.parseAction("DELETE"));
    }

    @Test
    void parseActionReturnsNullForUnknownToken() {
        assertNull(LootLockCommand.parseAction("not-an-action"));
        assertNull(LootLockCommand.parseAction(null));
    }

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
