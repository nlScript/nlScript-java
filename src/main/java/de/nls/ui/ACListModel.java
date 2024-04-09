package de.nls.ui;

import de.nls.core.Autocompletion;

import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;
import java.util.List;

public class ACListModel implements ListModel<Autocompletion> {

	private final List<Autocompletion> list = new ArrayList<>();

	private final ArrayList<ListDataListener> listeners = new ArrayList<>();

	public void add(Autocompletion s) {
		list.add(s);
		fireListChanged();
	}

	public void set(List<Autocompletion> s) {
		list.clear();
		list.addAll(s);
		fireListChanged();
	}

	@Override
	public int getSize() {
		return list.size();
	}

	@Override
	public Autocompletion getElementAt(int index) {
		return list.get(index);
	}

	@Override
	public void addListDataListener(ListDataListener l) {
		listeners.add(l);
	}

	@Override
	public void removeListDataListener(ListDataListener l) {
		listeners.remove(l);
	}

	private void fireListChanged() {
		ListDataEvent lda = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, list.size());
		for(ListDataListener l : listeners)
			l.contentsChanged(lda);
	}
}
