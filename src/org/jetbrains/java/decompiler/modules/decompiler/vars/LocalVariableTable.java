package org.jetbrains.java.decompiler.modules.decompiler.vars;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.java.decompiler.main.rels.MethodProcessorRunnable;
import org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement;
import org.jetbrains.java.decompiler.modules.decompiler.stats.Statement;

public class LocalVariableTable {
  private Map<StartEndPair, Set<LVTVariable>> endpoints;
  private ArrayList<LVTVariable> allLVT;
  private Map<Integer, List<LVTVariable>> mapLVT;

  public LocalVariableTable(int len) {
    endpoints = new HashMap<StartEndPair,Set<LVTVariable>>(len);
    allLVT = new ArrayList<LVTVariable>(len);
  }

  public void addVariable(LVTVariable v) {
    allLVT.add(v);
    v.addTo(endpoints);
  }

  public void mergeLVTs(LocalVariableTable otherLVT) {
   for (LVTVariable other : otherLVT.allLVT) {
      int idx = allLVT.indexOf(other);
      if (idx < 0) {
        allLVT.add(other);
      }
      else {
        LVTVariable mine = allLVT.get(idx);
        mine.merge(other);
      }
    }
    mapLVT = null; // Invalidate the cache and rebuild it.
  }

  public LVTVariable find(int index, Statement stat) {
    BitSet values = new BitSet();
    MethodProcessorRunnable.getOffset(stat, values);
    int start = values.nextSetBit(0);
    int end = values.length()-1;
    //System.out.println(indent + stat.getClass().getSimpleName() + " (" + start +", " + end + ")");

    Map<Integer, List<LVTVariable>> map = getMapVarNames();
    if (!map.containsKey(index)) {
      return null;
    }
    for (LVTVariable lvt : map.get(index)) {
      if (lvt.start >= start && lvt.end <= end) {
        return lvt;
      }
    }
    return null;
  }

  public Map<Integer, List<LVTVariable>> getMapVarNames() {
    if (mapLVT == null)
      buildNameMap();
    return mapLVT;
  }

  private void buildNameMap() {
    Map<Integer, Integer> versions = new HashMap<Integer, Integer>();
    mapLVT = new HashMap<Integer,List<LVTVariable>>();
    for (LVTVariable lvt : allLVT) {
      Integer idx = versions.get(lvt.index);
      if (idx == null)
        idx = 1;
      else
        idx++;
      versions.put(lvt.index, idx);
      List<LVTVariable> lvtList = mapLVT.get(lvt.index);
      if (lvtList == null) {
          lvtList = new ArrayList<LVTVariable>();
          mapLVT.put(lvt.index, lvtList);
      }
      lvtList.add(lvt);
    }
  }

  public List<LVTVariable> getCandidates(int index) {
    return getMapVarNames().get(index);
  }

  public Map<Integer, LVTVariable> getVars(Statement statement) {
      HashMap<Integer,LVTVariable> ret = new HashMap<Integer,LVTVariable>();
      if (statement == null) return ret;
      StartEndPair sepair = statement.getStartEndRange();
    if (endpoints.containsKey(sepair)) {
        for (LVTVariable lvt : endpoints.get(sepair)) {
            ret.put(lvt.index,lvt);
        }
    }
//    for (Entry<Integer, List<LVTVariable>> entry : endpoints.get(end)) {
//      for (LVTVariable lvt : entry.getValue()) {
//        if (lvt.start >= start && lvt.end <= end) {
//          if (ret.containsKey(entry.getKey())) {
//            System.out.println("DUPLICATE INDEX WHAT THE FUCK: " + entry.getKey());
//          }
//          ret.put(entry.getKey(), lvt);
//        }
//      }
//    }
    return ret;
  }
}