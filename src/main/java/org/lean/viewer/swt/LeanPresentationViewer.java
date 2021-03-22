package org.lean.viewer.swt;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.util.XMLResourceDescriptor;
import org.apache.commons.lang.StringUtils;
import org.apache.hop.core.SwtUniversalImageSvg;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.logging.ILoggingObject;
import org.apache.hop.core.svg.SvgImage;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.metadata.api.IHopMetadataProvider;
import org.apache.hop.metadata.api.IHopMetadataSerializer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.lean.core.draw.DrawnItem;
import org.lean.core.exception.LeanException;
import org.lean.presentation.LeanPresentation;
import org.lean.presentation.interaction.LeanInteraction;
import org.lean.presentation.interaction.LeanInteractionAction;
import org.lean.presentation.interaction.LeanInteractionMethod;
import org.lean.presentation.interaction.LeanInteractionParameter;
import org.lean.presentation.layout.LeanLayoutResults;
import org.lean.presentation.layout.LeanRenderPage;
import org.lean.presentation.page.LeanPage;
import org.lean.presentation.variable.LeanParameter;
import org.lean.render.IRenderContext;
import org.lean.render.context.PresentationRenderContext;
import org.w3c.dom.svg.SVGDocument;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class LeanPresentationViewer extends Composite implements PaintListener, MouseListener, MouseMoveListener {

  private final Composite parent;
  private final ILoggingObject loggingObject;
  private final IVariables variables;
  private final IHopMetadataProvider metadataProvider;

  private final Canvas wCanvas;

  // The results of the rendering of a presentation
  //
  private String currentPresentationName;
  private LeanPresentation currentPresentation;
  private LeanLayoutResults currentResults;
  private LeanRenderPage currentRenderPage;
  private String currentSvgXml;
  private SvgImage currentSvgImage;
  private List<LeanParameter> currentParameters;

  /**
   * Render a presentation and allow interactions with it.
   *
   * @param parent           The parent composite to use.
   * @param metadataProvider The metadata provider to use to load presentations and other objects
   * @param presentationName The starting presentation to render
   */
  public LeanPresentationViewer( Composite parent, ILoggingObject loggingObject, IVariables variables, IHopMetadataProvider metadataProvider, String presentationName )
    throws HopException, LeanException, IOException {
    super( parent, SWT.NO_BACKGROUND | SWT.NO_FOCUS | SWT.NO_MERGE_PAINTS | SWT.NO_RADIO_GROUP );
    this.loggingObject = loggingObject;
    this.parent = parent;
    this.variables = variables;
    this.metadataProvider = metadataProvider;
    this.currentPresentationName = presentationName;
    this.currentParameters = new ArrayList<>();

    setLayout( new FormLayout() );

    wCanvas = new Canvas( this, SWT.NONE );
    FormData fdCanvas = new FormData();
    fdCanvas.left = new FormAttachment( 0, 0 );
    fdCanvas.right = new FormAttachment( 100, 0 );
    fdCanvas.top = new FormAttachment( 0, 0 );
    fdCanvas.bottom = new FormAttachment( 100, 0 );
    wCanvas.setLayoutData( fdCanvas );

    // Load & render it
    loadAndRenderPresentation( presentationName );

    wCanvas.addPaintListener( this );
    wCanvas.addMouseListener( this );
    wCanvas.addMouseMoveListener( this );

  }

  private void loadAndRenderPresentation( String presentationName ) throws HopException, LeanException, IOException {
    // Let's load the presentation and render it then...
    //
    this.currentPresentationName = presentationName;
    IHopMetadataSerializer<LeanPresentation> presentationSerializer = metadataProvider.getSerializer( LeanPresentation.class );
    currentPresentation = presentationSerializer.load( presentationName );
    if ( currentPresentation == null ) {
      throw new HopException( "Unable to find presentation '" + presentationName + "'" );
    }

    IRenderContext renderContext = new PresentationRenderContext( currentPresentation );

    // Calculate the layout (also grabs data)
    currentResults = currentPresentation.doLayout( loggingObject, renderContext, metadataProvider, currentParameters );

    // render
    currentPresentation.render( currentResults, metadataProvider );

    if ( currentResults.getRenderPages().isEmpty() ) {
      throw new HopException( "There was no output after rendering (0 pages) of presentation " + presentationName );
    }

    // Take the first page...
    //
    currentRenderPage = currentResults.getRenderPages().get( 0 );

    // Get the XML
    //
    currentSvgXml = currentRenderPage.getSvgXml();

    // Load it back as a SvgDocument...
    //
    String parser = XMLResourceDescriptor.getXMLParserClassName();
    SAXSVGDocumentFactory f = new SAXSVGDocumentFactory( parser );
    SVGDocument document = f.createSVGDocument( "", new StringReader( currentSvgXml ) ); ;

    // Get it as an SVG image
    currentSvgImage = new SvgImage( document );
  }

  /**
   * Paint the presentation...
   *
   * @param paintEvent
   */
  @Override public void paintControl( PaintEvent paintEvent ) {
    // Simply render it onto the GC of the event...
    //
    try {
      LeanPage page = currentRenderPage.getPage();
      int width = page.getWidth();
      int height = page.getHeight();

      // Render the SVG image
      Image image = new SwtUniversalImageSvg( currentSvgImage, false )
        .getAsBitmapForSize( getParent().getDisplay(), width, height );

      // Double buffer the image on the canvas
      //
      paintEvent.gc.setBackground( new Color( getDisplay(), 255, 255, 255 ) );
      paintEvent.gc.setForeground( new Color( getDisplay(), 0, 0, 0 ) );
      paintEvent.gc.fillRectangle( 0, 0, width, height );

      paintEvent.gc.drawImage( image, 0, 0 );

      image.dispose();

    } catch ( Exception e ) {
      throw new RuntimeException( "Unable to get SVG XML from rendered page", e );
    }
  }

  @Override public void mouseDoubleClick( MouseEvent e ) {
    try {
      LeanInteractionMethod doubleClick = new LeanInteractionMethod( false, true );
      handleAction( e, doubleClick );
    } catch ( Exception ex ) {
      ex.printStackTrace();
    }

  }


  @Override public void mouseMove( MouseEvent e ) {

    // If we mouse over a possible interaction, change the cursor...
    //
    DrawnItem drawnItem = lookupDrawnItem( e.x, e.y );
    if ( drawnItem != null ) {
      // Find any interaction method that matches for the drawn item...
      //
      LeanInteraction interaction = currentPresentation.findInteraction( null, drawnItem );
      if ( interaction != null ) {
        setCursor( getDisplay().getSystemCursor( SWT.CURSOR_HAND ) );
        return;
      }
    }
    setCursor( getDisplay().getSystemCursor( SWT.CURSOR_ARROW ) );
  }

  private void handleAction( MouseEvent e, LeanInteractionMethod interactionMethod ) throws LeanException, IOException, HopException {
    DrawnItem drawnItem = lookupDrawnItem( e.x, e.y );
    if ( drawnItem != null ) {
      LeanInteraction interaction = currentPresentation.findInteraction( interactionMethod, drawnItem );
      if ( interaction != null ) {
        List<LeanInteractionAction> actions = interaction.getActions();
        for ( LeanInteractionAction action : actions ) {
          LeanInteractionAction.ActionType actionType = action.getActionType();

          switch ( actionType ) {
            case OpenPresentation:

              // The parameters...
              //
              currentParameters = new ArrayList<>();

              for ( LeanInteractionParameter parameter : action.getParameters() ) {

                LeanParameter leanParameter = new LeanParameter();
                leanParameter.setParameterName( parameter.getParameterName() );
                String value = "";
                switch ( parameter.getSourceType() ) {
                  case PresentationName:
                    value =
                      currentPresentation.getName();
                    break;
                  case ComponentName:
                    value =
                      drawnItem.getComponentName();
                    break;
                  case ItemType:
                    value = drawnItem.getType().name();
                    break;
                  case ItemValue:
                    if (drawnItem.getContext()!=null) {
                      value = drawnItem.getContext().getValue();
                    }
                    break;
                  case ItemCategory:
                    value = drawnItem.getCategory();
                    break;
                  case ComponentPluginId:
                    value = drawnItem.getComponentPluginId();
                    break;
                }
                leanParameter.setParameterValue( value );
                currentParameters.add( leanParameter );
              }

              if ( StringUtils.isNotEmpty( action.getObjectName() ) ) {
                loadAndRenderPresentation( action.getObjectName() );
                wCanvas.redraw();
              }
              break;
          }
        }
      }
    }
  }

  private DrawnItem lookupDrawnItem( int x, int y ) {
    DrawnItem drawnItem = currentRenderPage.lookupDrawnItem( x, y, true );
    if (drawnItem==null) {
      drawnItem = currentRenderPage.lookupDrawnItem( x, y, true );
    }
    return drawnItem;
  }

  @Override public void mouseDown( MouseEvent e ) {
    // System.out.println("MOUSE DOWN : ("+e.x+", "+e.y+")");
  }

  @Override public void mouseUp( MouseEvent e ) {
    try {
      LeanInteractionMethod click = new LeanInteractionMethod( true, false );
      handleAction( e, click );
    } catch ( Exception ex ) {
      ex.printStackTrace();
    }

  }

  /**
   * Gets parent
   *
   * @return value of parent
   */
  @Override public Composite getParent() {
    return parent;
  }

  /**
   * Gets variables
   *
   * @return value of variables
   */
  public IVariables getVariables() {
    return variables;
  }

  /**
   * Gets metadataProvider
   *
   * @return value of metadataProvider
   */
  public IHopMetadataProvider getMetadataProvider() {
    return metadataProvider;
  }

  /**
   * Gets presentationName
   *
   * @return value of presentationName
   */
  public String getCurrentPresentationName() {
    return currentPresentationName;
  }

  /**
   * Gets wCanvas
   *
   * @return value of wCanvas
   */
  public Canvas getwCanvas() {
    return wCanvas;
  }

  /**
   * Gets results
   *
   * @return value of results
   */
  public LeanLayoutResults getCurrentResults() {
    return currentResults;
  }

  /**
   * Gets renderPage
   *
   * @return value of renderPage
   */
  public LeanRenderPage getCurrentRenderPage() {
    return currentRenderPage;
  }
}
