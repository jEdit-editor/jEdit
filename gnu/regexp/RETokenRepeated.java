/*
 *  gnu/regexp/RETokenRepeated.java
 *  Copyright (C) 1998-2001 Wes Biggs
 *
 *  This library is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published
 *  by the Free Software Foundation; either version 2.1 of the License, or
 *  (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package gnu.regexp;
import java.util.Vector;

final class RETokenRepeated extends REToken {
    private REToken token;
    private int min,max;
    private boolean stingy;
    
    RETokenRepeated(int subIndex, REToken token, int min, int max) {
	super(subIndex);
	this.token = token;
	this.min = min;
	this.max = max;
    }
    
    void makeStingy() {
	stingy = true;
    }
    
    int getMinimumLength() {
	return (min * token.getMinimumLength());
    }

    // We do need to save every possible point, but the number of clone()
    // invocations here is really a killer for performance on non-stingy
    // repeat operators.  I'm open to suggestions...

    // Hypothetical question: can you have a RE that matches 1 times,
    // 3 times, 5 times, but not 2 times or 4 times?  Does having
    // the subexpression back-reference operator allow that?

    boolean match(CharIndexed input, REMatch mymatch) {
	int numRepeats = 0;
	
	// Possible positions for the next repeat to match at
	REMatch newMatch = mymatch;
	REMatch last = null;
	REMatch current;

	// Add the '0-repeats' index
	// positions.elementAt(z) == position [] in input after <<z>> matches
	Vector positions = new Vector();
	positions.addElement(newMatch);
	
	// Declare variables used in loop
	REMatch doables;
	REMatch doablesLast;
	REMatch recurrent;

	do {
	    // Check for stingy match for each possibility.
	    if (stingy && (numRepeats >= min)) {
		for (current = newMatch; current != null; current = current.next) {
		    if (next(input, current)) {
			mymatch.assignFrom(current);
			return true;
		    }
		}
	    }

	    doables = null;
	    doablesLast = null;

	    // try next repeat at all possible positions
	    for (current = newMatch; current != null; current = current.next) {
		recurrent = (REMatch) current.clone();
		if (token.match(input, recurrent)) {
		    // add all items in current to doables array
		    if (doables == null) {
			doables = recurrent;
			doablesLast = recurrent;
		    } else {
			// Order these from longest to shortest
			// Start by assuming longest (more repeats)
			doablesLast.next = recurrent;
		    }
		    // Find new doablesLast
		    while (doablesLast.next != null) {
			doablesLast = doablesLast.next;
		    }
		}
	    }
	    // if none of the possibilities worked out, break out of do/while
	    if (doables == null) break;
	    
	    // reassign where the next repeat can match
	    newMatch = doables;
	    
	    // increment how many repeats we've successfully found
	    ++numRepeats;
	    
	    positions.addElement(newMatch);
	} while (numRepeats < max);
	
	// If there aren't enough repeats, then fail
	if (numRepeats < min) return false;
	
	// We're greedy, but ease off until a true match is found 
	int posIndex = positions.size();
	
	// At this point we've either got too many or just the right amount.
	// See if this numRepeats works with the rest of the regexp.
	REMatch doneIndex = null;
	REMatch doneIndexLast = null;
	while (--posIndex >= min) {
	    newMatch = (REMatch) positions.elementAt(posIndex);
	    // If rest of pattern matches
	    // XXX has this already been tested?
	    for (current = newMatch; current != null; current = current.next) {
		// XXX why do we have to clone?
		recurrent = (REMatch) current.clone();
		if (next(input, recurrent)) {
		    // add all items in current to doneIndex array
		    if (doneIndex == null) {
			doneIndex = recurrent;
			doneIndexLast = recurrent;
		    } else {
			doneIndexLast.next = recurrent;
		    }
		    // Find new doneIndexLast
		    while (doneIndexLast.next != null) {
			doneIndexLast = doneIndexLast.next;
		    }
		}
	    }
	    // else did not match rest of the tokens, try again on smaller sample
	}
	if (doneIndex == null) {
	    return false;
	} else {
	    mymatch.assignFrom(doneIndex);
	    return true;
	}
    }

    void dump(StringBuffer os) {
	os.append("(?:");
	token.dumpAll(os);
	os.append(')');
	if ((max == Integer.MAX_VALUE) && (min <= 1))
	    os.append( (min == 0) ? '*' : '+' );
	else if ((min == 0) && (max == 1))
	    os.append('?');
	else {
	    os.append('{').append(min);
	    if (max > min) {
		os.append(',');
		if (max != Integer.MAX_VALUE) os.append(max);
	    }
	    os.append('}');
	}
	if (stingy) os.append('?');
    }
}
