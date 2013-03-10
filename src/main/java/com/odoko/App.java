package com.odoko;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.odoko.solrcli.actions.Action;
import com.odoko.solrcli.actions.ArgPostAction;
import com.odoko.solrcli.actions.FilePostAction;
import com.odoko.solrcli.actions.SearchAction;
import com.odoko.solrcli.actions.StdinPostAction;

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
  private Map<String, Action> actions = new HashMap<String, Action>();
  
  public static void main( String[] args ) throws Exception {
    new App(args).go();
  }
    
  public App(String[] args) {
	init();
    parseArgs(args);
  }

  public void init() {
	 actions.put("search", new SearchAction());
	 actions.put("files", new FilePostAction());
	 actions.put("stdin", new StdinPostAction());
	 actions.put("post", new ArgPostAction());
	 //actions.put("crawl", new CrawlPostAction());
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
    if (actions.containsKey(actionname)) {
      action = actions.get(actionname);
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
