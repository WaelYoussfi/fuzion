/*

This file is part of the Fuzion language implementation.

The Fuzion language implementation is free software: you can redistribute it
and/or modify it under the terms of the GNU General Public License as published
by the Free Software Foundation, version 3 of the License.

The Fuzion language implementation is distributed in the hope that it will be
useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public
License for more details.

You should have received a copy of the GNU General Public License along with The
Fuzion language implementation.  If not, see <https://www.gnu.org/licenses/>.

*/

/*-----------------------------------------------------------------------
 *
 * Tokiwa Software GmbH, Germany
 *
 * Source of class Instance
 *
 *---------------------------------------------------------------------*/

package dev.flang.fuir.analysis;

import java.util.TreeMap;


/**
 * Instance represents an abstract instance of a feature handled by the DFA
 * Analysis. An Abstract instance may consist of abstract values as well as
 * context information, taint information, etc.
 *
 * @author Fridtjof Siebert (siebert@tokiwa.software)
 */
public class Instance extends Value implements Comparable<Instance> // , Context
{


  /*-----------------------------  classes  -----------------------------*/


  /*----------------------------  constants  ----------------------------*/


  /*----------------------------  variables  ----------------------------*/


  /**
   * The clazz this is an instance of.
   */
  int _clazz;


  /**
   * The DFA instance we are working with.
   */
  DFA _dfa;


  /**
   * Map from fields to the values that have been assigned to the fields.
   */
  final TreeMap<Integer, Value> _fields;


  /**
   * For debugging: Reason that causes this instance to be part of the analysis.
   */
  Context _context;


  /**
   * Is this instance the result of the IR command 'Box'.  If so, there is a
   * terrible hack to find the field values.
   */
  final boolean _isBoxed;


  /*---------------------------  consructors  ---------------------------*/


  /**
   * Create Instance of given clazz
   *
   * @param dfa the DFA instance we are analyzing with
   *
   * @param clazz the clazz this is an instance of.
   *
   * @param context for debugging: Reason that causes this instance to be part
   * of the analysis.
   */
  public Instance(DFA dfa, int clazz, Context context)
  {
    _clazz = clazz;
    _dfa = dfa;
    _context = context;
    _fields = new TreeMap<>();
    _isBoxed = false;
  }


  /**
   * Create boxed Instance of given value
   *
   * @param original the original value.
   */
  Instance(Instance original, int vc, int rc)
  {
    if (PRECONDITIONS) require
      (original._clazz == vc);

    _clazz = rc;
    _dfa = original._dfa;
    _context = original._context;
    _fields = (TreeMap<Integer, Value>) original._fields.clone();
    _isBoxed = true;
  }


  /*-----------------------------  methods  -----------------------------*/


  /**
   * Compare this to another instance.
   */
  public int compareTo(Instance other)
  {
    return
      _clazz < other._clazz ? -1 :
      _clazz > other._clazz ? +1 : 0;
  }


  /**
   * Add v to the set of values of given field within this instance.
   */
  public void setField(int field, Value v)
  {
    if (PRECONDITIONS) require
      (v != null);

    var oldv = _fields.get(field);
    if (oldv != null)
      {
        v = oldv.join(v);
      }
    _fields.put(field, v);
  }


  /**
   * Get set of values of given field within this instance.
   */
  Value readFieldFromInstance(DFA dfa, int target, int field)
  {
    if (PRECONDITIONS) require
      (_clazz == target);

    var v = _fields.get(field);
    if (v == null && _isBoxed)
      {
        for (var f : _fields.keySet())
          { // NYI: HACK: For a boxed value, we read the corresponding value
            // type field. We should better copy all the fields over from the
            // value type to the ref type.
            if (dfa._fuir.clazzAsString(f).equals(dfa._fuir.clazzAsString(field).replace("ref ","")))
              {
                v = _fields.get(f);
              }
          }
      }
    if (v == null)
      {
        if (dfa._reportResults)
          {
            System.err.println("*** reading uninitialized field " + dfa._fuir.clazzAsString(field));
            for (var f : _fields.keySet())
              {
                if (dfa._fuir.clazzAsString(f).equals(dfa._fuir.clazzAsString(field).replace("ref ","")))
                  {
                    System.out.println("NYI: HACK: Using value version instead: "+v);
                  }
              }
          }
      }
    return v;
  }


  /**
   * Create the union of the values 'this' and 'v'. This is called by join()
   * after common cases (same instnace, UNDEFINED) have been handled.
   */
  public Value joinInstances(Value v)
  {
    return new ValueSet(this, v);
  }


  /**
   * Create human-readable string from this instance.
   */
  public String toString()
  {
    return _dfa._fuir.clazzAsString(_clazz);
  }

  /**
   * Show the context that caused the inclusion of this instance into the analysis.
  public String showWhy()
  {
    var indent = _context.showWhy();
    System.out.println(indent + "  |");
    System.out.println(indent + "  +- creates Instance " + this);
    return indent + "  ";
  }
   */


  /**
   * Create a boxed (ref) value of this instance.
   */
  Value box(DFA dfa, int vc, int rc)
  {
    if (CHECKS) check
      (_clazz == vc);

    return new Instance(this, vc, rc);
  }


}

/* end of file */
