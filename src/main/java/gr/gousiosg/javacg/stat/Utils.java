package gr.gousiosg.javacg.stat;

import java.lang.StringBuilder;


public class Utils {

  public static String strJoin(String[] aArr, String sSep) {
    StringBuilder sbStr = new StringBuilder();
    for (int i = 0, il = aArr.length; i < il; i++) {
      if(aArr[i] != null && !aArr[i].isEmpty()){
        if (i > 0)
          sbStr.append(sSep);
        sbStr.append(aArr[i]);
      }
    }
    return sbStr.toString();
  }
}
