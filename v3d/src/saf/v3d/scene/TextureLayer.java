/**
 * 
 */
package saf.v3d.scene;

import javax.media.opengl.GL2;

import saf.v3d.render.RenderState;

/**
 * Layer that contains TextureLayers. This sets the opengl state necessary for
 * drawing any texture and then draws the individual textures.
 * 
 * @author Nick Collier
 */
public class TextureLayer extends VComposite {
  
  public TextureLayer() {
    children = new TextureLayerCollection();
  }
  
  
  /* (non-Javadoc)
   * @see saf.v3d.scene.VComposite#invalidate(int)
   */
  @Override
  public void invalidate(GL2 gl) {
    ((TextureLayerCollection)children).invalidate(gl);
  }


  /*
   * (non-Javadoc)
   * 
   * @see saf.v3d.scene.VSpatial#draw(javax.media.opengl.GL,
   * saf.v3d.render.RenderState)
   */
  @Override
  public void draw(GL2 gl, RenderState state) {
    //gl.glEnable(GL.GL_TEXTURE_2D);
    //gl.glEnable(GL.GL_BLEND);
    //gl.glBlendFunc(GL2.GL_ONE, GL.GL_ONE_MINUS_SRC_ALPHA);
    //gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

    //gl.glEnable(GL2.GL_ALPHA_TEST);
    //gl.glAlphaFunc(GL.GL_GREATER, 0); 
    
    ((TextureLayerCollection)children).draw(gl, state);
    
    //gl.glDisable(GL.GL_BLEND);
    //gl.glDisable(GL.GL_TEXTURE_2D);
  }
}
