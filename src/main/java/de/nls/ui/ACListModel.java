package de.nls.ui;

import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ACListModel implements ListModel<IAutocompletion> {

	private final List<IAutocompletion> list = new ArrayList<>();

	private final ArrayList<ListDataListener> listeners = new ArrayList<>();

	public void add(IAutocompletion s) {
		list.add(s);
		fireListChanged();
	}

	public void set(IAutocompletion... s) {
		list.clear();
		list.addAll(Arrays.asList(s));
		fireListChanged();
	}

	@Override
	public int getSize() {
		return list.size();
	}

	@Override
	public IAutocompletion getElementAt(int index) {
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
