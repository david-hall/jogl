/*
 * Copyright (c) 2003-2005 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 * 
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package com.sun.javafx.newt.impl;

import java.security.*;

/** Helper routines for logging and debugging. */

public class Debug {
  // Some common properties
  private static boolean verbose;
  private static boolean debugAll;
  
  static {
    verbose = isPropertyDefined("newt.verbose");
    debugAll = isPropertyDefined("newt.debug");
    if (verbose) {
       Package p = Package.getPackage("com.sun.javafx.newt");
       System.err.println("NEWT specification version " + p.getSpecificationVersion());
       System.err.println("NEWT implementation version " + p.getImplementationVersion());
       System.err.println("NEWT implementation vendor " + p.getImplementationVendor());
    }
  }

  public static int getIntProperty(final String property, final boolean jnlpAlias) {
    int i=0;
    try {
        Integer iv = Integer.valueOf(Debug.getProperty(property, jnlpAlias));
        i = iv.intValue();
    } catch (NumberFormatException nfe) {}
    return i;
  }

  public static boolean getBooleanProperty(final String property, final boolean jnlpAlias) {
    Boolean b = Boolean.valueOf(Debug.getProperty(property, jnlpAlias));
    return b.booleanValue();
  }

  public static boolean isPropertyDefined(final String property) {
    return (Debug.getProperty(property, true) != null) ? true : false;
  }

  public static String getProperty(final String property, final boolean jnlpAlias) {
    String s = (String) AccessController.doPrivileged(new PrivilegedAction() {
        public Object run() {
          String val = System.getProperty(property);
          if(null==val && jnlpAlias && !property.startsWith(jnlp_prefix)) {
              val = System.getProperty(jnlp_prefix + property);
          }
          return val;
        }
      });
    return s;
  }
  public static final String jnlp_prefix = "jnlp." ;

  public static boolean verbose() {
    return verbose;
  }

  public static boolean debugAll() {
    return debugAll;
  }

  public static boolean debug(String subcomponent) {
    return debugAll() || isPropertyDefined("newt.debug." + subcomponent);
  }
}
