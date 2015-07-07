package org.robotframework.ide.eclipse.main.plugin.tableeditor.keywords.handler;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.robotframework.ide.eclipse.main.plugin.RobotElement;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.FocusedViewerAccessor;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.RobotEditorCommandsStack;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.keywords.handler.DeleteCellContentHandler.E4DeleteCellContentHandler;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.keywords.handler.PasteCellContentHandler.E4PasteCellContentHandler;
import org.robotframework.viewers.Selections;

public class PasteCellContentHandler extends DIHandler<E4PasteCellContentHandler> {

    public PasteCellContentHandler() {
        super(E4PasteCellContentHandler.class);
    }

    public static class E4PasteCellContentHandler {

        @Inject
        private RobotEditorCommandsStack commandsStack;

        @Execute
        public Object pasteContent(@Named(Selections.SELECTION) final IStructuredSelection selection,
                final FocusedViewerAccessor viewerAccessor, final Clipboard clipboard) {

            final String contents = (String) clipboard.getContents(TextTransfer.getInstance());

            final RobotElement element = Selections.getSingleElement(selection, RobotElement.class);
            final int index = viewerAccessor.getFocusedCell().getColumnIndex();
            final int noOfColumns = ((TreeViewer) viewerAccessor.getViewer()).getTree().getColumnCount();

            E4DeleteCellContentHandler.setAttributeAtPosition(commandsStack, element, index, noOfColumns, contents);

            return null;
        }
    }
}
