package com.odoko;

import java.util.List;
import java.util.Map;

public abstract class AbstractAction implements Action {

  protected List<String> arguments;
  protected Map<String, String> options;
    
  @Override
  public void configure(List<String> arguments, Map<String, String> options) {
    this.arguments = arguments;
    this.options = options;
  }

  protected boolean parseBoolean(String str) {
    str = str.toLowerCase().trim();
    return str.equals("true") || str.equals("yes") || str.equals("on");
  }
}
