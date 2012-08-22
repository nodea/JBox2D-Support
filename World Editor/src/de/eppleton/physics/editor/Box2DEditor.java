/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.eppleton.physics.editor;

import de.eppleton.jbox2d.WorldUtilities;
import de.eppleton.physics.editor.Box2DDataObject.ViewSynchronizer;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.jbox2d.dynamics.World;
import org.netbeans.core.spi.multiview.text.MultiViewEditorElement;
import org.openide.text.DataEditorSupport;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;

/**
 *
 * @author antonepple
 */
public class Box2DEditor extends MultiViewEditorElement implements PropertyChangeListener {

    private Updater updater;
    private DocumentListenerImpl documentListenerImpl;
    private Document document;
    private Lookup lkp;
    private ViewSynchronizer synchronizer;

//    private Document document;
    public Box2DEditor(Lookup lookup) {
        super(lookup);
        lkp = lookup;
        updater = new Updater(this);
    }

    @Override
    public void componentActivated() {
        super.componentActivated();
        DataEditorSupport cookie = getLookup().lookup(DataEditorSupport.class);
        document = cookie.getOpenedPanes()[0].getDocument();
        documentListenerImpl = new DocumentListenerImpl();
        document.addDocumentListener(documentListenerImpl);
        synchronizer = lkp.lookup(Box2DDataObject.ViewSynchronizer.class);
        synchronizer.addPropertyChangeListener(this);
    }

    public void updateDocument(World world) {
        document.removeDocumentListener(documentListenerImpl);
        try {
            document.remove(document.getStartPosition().getOffset(), document.getLength());
            document.insertString(0, WorldUtilities.serializeWorld(world), null);
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        }
        document.addDocumentListener(documentListenerImpl);

    }

    private void updateFromDocument() {
        synchronizer.removePropertyChangelistener(this);
        World parsedWorld  = null;
        try {
            parsedWorld = WorldUtilities.parseWorld(document.getText(0, document.getLength()));
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        }
        if (parsedWorld != null) {
            synchronizer.setWorld(null);
        }
        synchronizer.addPropertyChangeListener(this);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName() == Box2DDataObject.ViewSynchronizer.WORLD_CHANGED) {
            updateDocument((World) evt.getNewValue());
        }
    }

    private static class Updater implements Runnable {

        private static final RequestProcessor RP = new RequestProcessor(Updater.class);
        private final RequestProcessor.Task UPDATE = RP.create(this);
        private final Box2DEditor editorElementImpl;

        public Updater(Box2DEditor bddo) {
            this.editorElementImpl = bddo;
        }

        public void documentChange() {
            UPDATE.schedule(1000);
        }

        public void run() {
            editorElementImpl.updateFromDocument();
        }
    }

    private class DocumentListenerImpl implements DocumentListener {

        public DocumentListenerImpl() {
        }

        @Override
        public void insertUpdate(DocumentEvent de) {
            updater.documentChange();
        }

        @Override
        public void removeUpdate(DocumentEvent de) {
            updater.documentChange();
        }

        @Override
        public void changedUpdate(DocumentEvent de) {
            updater.documentChange();
        }
    }
}