package com.gitbitex.matchingengine;

import com.gitbitex.matchingengine.command.Command;

public interface EngineListener {
    void onCommandExecuted(Command command, ModifiedObjectList modifiedObjects);
}
