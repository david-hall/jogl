package com.jogamp.opengl.test.junit.jogl.util.texture;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.jogl.demos.TextureSequenceDemo01;
import com.jogamp.opengl.test.junit.jogl.demos.es2.TextureSequenceCubeES2;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;

public class TestTextureSequence01NEWT extends UITestCase {
    static boolean showFPS = false;
    static int width = 510;
    static int height = 300;
    static boolean useBuildInTexLookup = false;
    static long duration = 500; // ms
    static GLProfile glp;
    static GLCapabilities caps;
    
    @BeforeClass
    public static void initClass() {
        glp = GLProfile.getGL2ES2();
        Assert.assertNotNull(glp);
        caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);
    }

    void testImpl() throws InterruptedException {
        final GLWindow window = GLWindow.create(caps);
        window.setTitle("TestTextureSequence01NEWT");
        // Size OpenGL to Video Surface
        window.setSize(width, height);
        window.setFullscreen(false);
        window.setSize(width, height);
        final TextureSequenceDemo01 texSource = new TextureSequenceDemo01(useBuildInTexLookup);
        window.addGLEventListener(new GLEventListener() {
            @Override
            public void init(GLAutoDrawable drawable) {
                texSource.initGLResources(drawable.getGL());
            }
            @Override
            public void dispose(GLAutoDrawable drawable) { }
            @Override
            public void display(GLAutoDrawable drawable) { }
            @Override
            public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) { }            
        });
        window.addGLEventListener(new TextureSequenceCubeES2(texSource, false, -2.3f, 0f, 0f));
        final Animator animator = new Animator(window);
        animator.setUpdateFPSFrames(60, showFPS ? System.err : null);
        QuitAdapter quitAdapter = new QuitAdapter();
        window.addKeyListener(quitAdapter);
        window.addWindowListener(quitAdapter);
        animator.start();
        window.setVisible(true);
        
        while(!quitAdapter.shouldQuit() && animator.isAnimating() && animator.getTotalFPSDuration()<duration) {
            Thread.sleep(100);
        }
        
        animator.stop();
        Assert.assertFalse(animator.isAnimating());
        Assert.assertFalse(animator.isStarted());
        window.destroy();
    }
    
    @Test
    public void test1() throws InterruptedException {
        testImpl();        
    }
    
    public static void main(String[] args) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            } else if(args[i].equals("-width")) {
                i++;
                width = MiscUtils.atoi(args[i], width);
            } else if(args[i].equals("-height")) {
                i++;
                height = MiscUtils.atoi(args[i], height);
            } else if(args[i].equals("-shaderBuildIn")) {
                useBuildInTexLookup = true;
            }
        }
        org.junit.runner.JUnitCore.main(TestTextureSequence01NEWT.class.getName());        
    }

}
