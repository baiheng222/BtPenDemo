package com.nordicsemi.nrfUARTv2.drawutil;


public interface IUndoCommand
{
    public void undo();
    public void redo();
    public boolean canUndo();
    public boolean canRedo();
    public void onDeleteFromUndoStack();
    public void onDeleteFromRedoStack();
}
