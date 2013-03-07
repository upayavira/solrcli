package com.odoko;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Initial actions:
 *   - clone post.jar
 *   - multithreading
 *   - basic search
 *   - deletions
 *   - indexer.jar
 *   
 *  Make a simple search the first option - would have just been useful to me.
 *  So, how does that happen?
 *  I need a CLI parser. No one will object to commons CLI. 
 *  
 */
public class App 
{
  private String actionname = null;
  private Map<String,String> options = new HashMap<String,String>();
  private List<String> arguments = new ArrayList<String>();
 
  public static void main( String[] args ) throws Exception {
    new App(args).go();
  }
    
  public App(String[] args) {
    parseArgs(args);
  }

  private void parseArgs(String[] args) {
    for (int i=0; i<args.length; i++) {
      String arg = args[i];
      if (arg.startsWith("-")) {
        arg=arg.substring(1);
        if (arg.contains("=")) {
          String[]parts = arg.split("=",2);
          String name = parts[0];
          String value = parts[1];
          options.put(name,  value);
        } else {
          options.put(arg, null);
        }
      } else if (actionname == null) {
        actionname = arg;
      } else {
        arguments.add(arg);
      }
    }
  }

  private Action getAction() {
    Action action;
    if (actionname.equals("search")) {
      action = new SearchAction();
    } else {
      throw new RuntimeException("Unknown action: " + actionname);
    }
    action.configure(arguments, options);
    return action;
  }
  
  private void go() {
    Action action = getAction();
    action.init();
    action.go();
  }
}
