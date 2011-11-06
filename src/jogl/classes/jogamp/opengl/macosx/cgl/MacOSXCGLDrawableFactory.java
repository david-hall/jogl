/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
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

package jogamp.opengl.macosx.cgl;

import java.nio.Buffer;
import java.util.HashMap;
import java.util.List;

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.AbstractGraphicsScreen;
import javax.media.nativewindow.DefaultGraphicsScreen;
import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.NativeWindow;
import javax.media.nativewindow.NativeWindowFactory;
import javax.media.nativewindow.ProxySurface;
import javax.media.nativewindow.macosx.MacOSXGraphicsDevice;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesChooser;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;

import jogamp.nativewindow.WrappedSurface;
import jogamp.nativewindow.jawt.macosx.MacOSXJAWTWindow;
import jogamp.opengl.DesktopGLDynamicLookupHelper;
import jogamp.opengl.GLDrawableFactoryImpl;
import jogamp.opengl.GLDrawableImpl;
import jogamp.opengl.GLDynamicLookupHelper;
import jogamp.opengl.GLGraphicsConfigurationUtil;

import com.jogamp.common.JogampRuntimeException;
import com.jogamp.common.util.ReflectionUtil;

public class MacOSXCGLDrawableFactory extends GLDrawableFactoryImpl {
  private static final DesktopGLDynamicLookupHelper macOSXCGLDynamicLookupHelper;

  static {
        DesktopGLDynamicLookupHelper tmp = null;
        try {
            tmp = new DesktopGLDynamicLookupHelper(new MacOSXCGLDynamicLibraryBundleInfo());
        } catch (GLException gle) {
            if(DEBUG) {
                gle.printStackTrace();
            }
        }
        macOSXCGLDynamicLookupHelper = tmp;
        /** FIXME ?? 
        if(null!=macOSXCGLDynamicLookupHelper) {
            CGL.getCGLProcAddressTable().reset(macOSXCGLDynamicLookupHelper);
        } */
  }

  public GLDynamicLookupHelper getGLDynamicLookupHelper(int profile) {
      return macOSXCGLDynamicLookupHelper;
  }

  public MacOSXCGLDrawableFactory() {
    super();

    // Register our GraphicsConfigurationFactory implementations
    // The act of constructing them causes them to be registered
    new MacOSXCGLGraphicsConfigurationFactory();
    if(GLProfile.isAWTAvailable()) {
        try {
          ReflectionUtil.createInstance("jogamp.opengl.macosx.cgl.awt.MacOSXAWTCGLGraphicsConfigurationFactory",
                                        null, getClass().getClassLoader());
        } catch (JogampRuntimeException jre) { /* n/a .. */ }
    }

    defaultDevice = new MacOSXGraphicsDevice(AbstractGraphicsDevice.DEFAULT_UNIT);
  }

  static class SharedResource {
      // private MacOSXCGLDrawable drawable;
      // private MacOSXCGLContext context;
      MacOSXGraphicsDevice device;
      boolean wasContextCreated;

      SharedResource(MacOSXGraphicsDevice device, boolean wasContextCreated
                     /* MacOSXCGLDrawable draw, MacOSXCGLContext ctx */) {
          // drawable = draw;
          // context = ctx;
          this.device = device;
          this.wasContextCreated = wasContextCreated;
      }
      final MacOSXGraphicsDevice getDevice() { return device; }
      final boolean wasContextAvailable() { return wasContextCreated; }
  }
  HashMap/*<connection, SharedResource>*/ sharedMap = new HashMap();
  MacOSXGraphicsDevice defaultDevice;

  public final AbstractGraphicsDevice getDefaultDevice() {
      return defaultDevice;
  }

  public final boolean getIsDeviceCompatible(AbstractGraphicsDevice device) {
      if(device instanceof MacOSXGraphicsDevice) {
          return true;
      }
      return false;
  }

  private boolean isOSXContextAvailable(AbstractGraphicsDevice sharedDevice) {
    boolean madeCurrent = false;
    GLProfile glp = GLProfile.get(sharedDevice, GLProfile.GL_PROFILE_LIST_MIN_DESKTOP);
    if (null == glp) {
        throw new GLException("Couldn't get default GLProfile for device: "+sharedDevice);
    }    
    final GLCapabilities caps = new GLCapabilities(glp);
    caps.setRedBits(5); caps.setGreenBits(5); caps.setBlueBits(5); caps.setAlphaBits(0);
    caps.setDoubleBuffered(false);
    caps.setOnscreen(false);
    caps.setPBuffer(true);
    final MacOSXCGLDrawable drawable = (MacOSXCGLDrawable) createGLDrawable( createOffscreenSurfaceImpl(sharedDevice, caps, caps, null, 64, 64) );        
    if(null!=drawable) {
        final GLContext context = drawable.createContext(null);
        if (null != context) {
            context.setSynchronized(true);
            try {
                context.makeCurrent(); // could cause exception
                madeCurrent = context.isCurrent();
            } catch (GLException gle) {
                if (DEBUG) {
                    System.err.println("MacOSXCGLDrawableFactory.createShared: INFO: makeCurrent failed");
                    gle.printStackTrace();
                }
            } finally {
                context.destroy();
            }
        }
        drawable.destroy();
    }
    return madeCurrent;
  }
  
  /* package */ SharedResource getOrCreateOSXSharedResource(AbstractGraphicsDevice adevice) {
    String connection = adevice.getConnection();
    SharedResource sr;
    synchronized(sharedMap) {
        sr = (SharedResource) sharedMap.get(connection);
    }
    if(null==sr) {
        final MacOSXGraphicsDevice sharedDevice = new MacOSXGraphicsDevice(adevice.getUnitID());
        final boolean madeCurrent = isOSXContextAvailable(sharedDevice);
        sr = new SharedResource(sharedDevice, madeCurrent);
        synchronized(sharedMap) {
            sharedMap.put(connection, sr);
        }
        if (DEBUG) {
            System.err.println("MacOSXCGLDrawableFactory.createShared: device:  " + sharedDevice);
            System.err.println("MacOSXCGLDrawableFactory.createShared: context: " + madeCurrent);
        }                        
    }
    return sr;
  }
    
  public final boolean getWasSharedContextCreated(AbstractGraphicsDevice device) {
    SharedResource sr = getOrCreateOSXSharedResource(device);
    if(null!=sr) {
        return sr.wasContextAvailable();
    }
    return false;        
  }
  
  protected final GLContext getOrCreateSharedContextImpl(AbstractGraphicsDevice device) {
      // FIXME: not implemented .. needs a dummy OSX surface
      return null;
  }

  protected AbstractGraphicsDevice getOrCreateSharedDeviceImpl(AbstractGraphicsDevice device) {
      SharedResource sr = getOrCreateOSXSharedResource(device);
      if(null!=sr) {
          return sr.getDevice();
      }
      return null;
  }

  protected final void shutdownInstance() {}

  protected List<GLCapabilitiesImmutable> getAvailableCapabilitiesImpl(AbstractGraphicsDevice device) {
      return MacOSXCGLGraphicsConfiguration.getAvailableCapabilities(this, device);
  }

  protected static MacOSXJAWTWindow getLayeredSurfaceHost(NativeSurface surface) {
      if(surface instanceof NativeWindow) {
          final NativeWindow nwThis = (NativeWindow) surface;
          if( nwThis instanceof MacOSXJAWTWindow) {
              // direct surface host, eg. via AWT GLCanvas
              final MacOSXJAWTWindow r = (MacOSXJAWTWindow) nwThis;
              return r.isLayeredSurface() ? r : null;
          } else {
              // parent surface host, eg. via native parenting w/ NewtCanvasAWT
              final NativeWindow nwParent = nwThis.getParent();
              if(null != nwParent && nwParent instanceof MacOSXJAWTWindow) {
                  final MacOSXJAWTWindow r = (MacOSXJAWTWindow) nwParent;
                  return r.isLayeredSurface() ? r : null;
              }
          }
      }
      return null;      
  }
      
  protected GLDrawableImpl createOnscreenDrawableImpl(NativeSurface target) {
    if (target == null) {
      throw new IllegalArgumentException("Null target");
    }
    final MacOSXJAWTWindow lsh = MacOSXCGLDrawableFactory.getLayeredSurfaceHost(target);
    if(null != lsh) {
        // layered surface -> PBuffer
        final MacOSXCGLGraphicsConfiguration config = (MacOSXCGLGraphicsConfiguration) target.getGraphicsConfiguration().getNativeGraphicsConfiguration();        
        final GLCapabilitiesImmutable chosenCaps = GLGraphicsConfigurationUtil.fixGLPBufferGLCapabilities((GLCapabilitiesImmutable) config.getChosenCapabilities());
        config.setChosenCapabilities(chosenCaps);
        return new MacOSXPbufferCGLDrawable(this, target, false);
    }
    return new MacOSXOnscreenCGLDrawable(this, target);
  }

  protected GLDrawableImpl createOffscreenDrawableImpl(NativeSurface target) {
    AbstractGraphicsConfiguration config = target.getGraphicsConfiguration().getNativeGraphicsConfiguration();
    GLCapabilitiesImmutable caps = (GLCapabilitiesImmutable) config.getChosenCapabilities();
    if(!caps.isPBuffer()) {
        return new MacOSXOffscreenCGLDrawable(this, target);
    }

    // PBuffer GLDrawable Creation
    /**
     * FIXME: Think about this ..
     * should not be necessary ? ..
    final List returnList = new ArrayList();
    final GLDrawableFactory factory = this;
    Runnable r = new Runnable() {
        public void run() {
          returnList.add(new MacOSXPbufferCGLDrawable(factory, target));
        }
      };
    maybeDoSingleThreadedWorkaround(r);
    return (GLDrawableImpl) returnList.get(0);
    */
    return new MacOSXPbufferCGLDrawable(this, target, true);
  }

  public boolean canCreateGLPbuffer(AbstractGraphicsDevice device) {
    return true;
  }

  protected NativeSurface createOffscreenSurfaceImpl(AbstractGraphicsDevice device,GLCapabilitiesImmutable capsChosen, GLCapabilitiesImmutable capsRequested, GLCapabilitiesChooser chooser, int width, int height) {
    AbstractGraphicsScreen screen = DefaultGraphicsScreen.createDefault(NativeWindowFactory.TYPE_MACOSX);
    WrappedSurface ns = new WrappedSurface(MacOSXCGLGraphicsConfigurationFactory.chooseGraphicsConfigurationStatic(capsChosen, capsRequested, chooser, screen, true));
    ns.setSize(width, height);
    return ns;
  }

  protected ProxySurface createProxySurfaceImpl(AbstractGraphicsDevice device, long windowHandle, GLCapabilitiesImmutable capsRequested, GLCapabilitiesChooser chooser) {
    AbstractGraphicsScreen screen = new DefaultGraphicsScreen(device, 0);    
    WrappedSurface ns = new WrappedSurface(MacOSXCGLGraphicsConfigurationFactory.chooseGraphicsConfigurationStatic(capsRequested, capsRequested, chooser, screen, true), windowHandle);
    return ns;    
  }  
  
  protected GLContext createExternalGLContextImpl() {
    return MacOSXExternalCGLContext.create(this);
  }

  public boolean canCreateExternalGLDrawable(AbstractGraphicsDevice device) {
    return false;
  }

  protected GLDrawable createExternalGLDrawableImpl() {
    // FIXME
    throw new GLException("Not yet implemented");
  }

  public boolean canCreateContextOnJava2DSurface(AbstractGraphicsDevice device) {
    return false;
  }

  public GLContext createContextOnJava2DSurface(Object graphics, GLContext shareWith)
    throws GLException {
    throw new GLException("not supported in non AWT enviroment");
  }
  
  //------------------------------------------------------
  // Gamma-related functionality
  //

  private static final int GAMMA_RAMP_LENGTH = 256;

  /** Returns the length of the computed gamma ramp for this OS and
      hardware. Returns 0 if gamma changes are not supported. */
  protected int getGammaRampLength() {
    return GAMMA_RAMP_LENGTH;
  }

  protected boolean setGammaRamp(float[] ramp) {
    return CGL.setGammaRamp(ramp.length,
                            ramp, 0,
                            ramp, 0,
                            ramp, 0);
  }

  protected Buffer getGammaRamp() {
    return null;
  }

  protected void resetGammaRamp(Buffer originalGammaRamp) {
    CGL.resetGammaRamp();
  }
}
