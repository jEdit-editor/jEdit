package org.gjt.sp.util;

import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.util.Deque;
import java.util.LinkedList;

/**
 * A model for a single String, which supports TextListeners. Simpler to use
 * than Document when all you need is to store a single string.
 * 
 * @author ezust
 */
public class StringModel
{
	private String theText;

	private Deque<TextListener> listeners = new LinkedList<>();

	public void addTextListener(TextListener tl)
	{
		listeners.add(tl);
	}

	void removeTextListener(TextListener tl)
	{
		listeners.remove(tl);
	}

	void fireTextChanged()
	{
		TextEvent te = new TextEvent(this, TextEvent.TEXT_VALUE_CHANGED);
		for (TextListener listener : listeners)
			listener.textValueChanged(te);
	}

	public String toString()
	{
		return theText;
	}

	public void setText(String newText)
	{
		this.theText = newText;
		fireTextChanged();
	}
}
