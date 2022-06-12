package org.opetryna.xmleditor

interface Command {
    fun execute()
    fun undo()
}

class CommandHistory {

    private val historyUndo = ArrayDeque<Command>()
    private val historyRedo = ArrayDeque<Command>()

    fun execute(cmd: Command) {
        cmd.execute()
        historyUndo.addLast(cmd)
        historyRedo.clear()
    }

    fun undo() {
        val cmd = historyUndo.removeLastOrNull()
        if (cmd != null) {
            cmd.undo()
            historyRedo.addLast(cmd)
        }
    }

    fun redo() {
        val cmd = historyRedo.removeLastOrNull()
        if (cmd != null) {
            cmd.execute()
            historyUndo.addLast(cmd)
        }
    }

    fun clear() {
        historyUndo.clear()
        historyRedo.clear()
    }

}