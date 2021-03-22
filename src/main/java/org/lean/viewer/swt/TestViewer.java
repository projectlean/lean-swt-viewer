package org.lean.viewer.swt;

import org.apache.hop.core.HopEnvironment;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.logging.LoggingObjectType;
import org.apache.hop.core.logging.SimpleLoggingObject;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.core.variables.Variables;
import org.apache.hop.metadata.api.IHopMetadataProvider;
import org.apache.hop.metadata.api.IHopMetadataSerializer;
import org.apache.hop.metadata.serializer.memory.MemoryMetadataProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.lean.core.LeanAttachment;
import org.lean.core.LeanEnvironment;
import org.lean.core.LeanFont;
import org.lean.core.LeanHorizontalAlignment;
import org.lean.core.LeanVerticalAlignment;
import org.lean.presentation.LeanPresentation;
import org.lean.presentation.component.LeanComponent;
import org.lean.presentation.component.types.label.LeanLabelComponent;
import org.lean.presentation.interaction.LeanInteraction;
import org.lean.presentation.interaction.LeanInteractionAction;
import org.lean.presentation.interaction.LeanInteractionLocation;
import org.lean.presentation.interaction.LeanInteractionMethod;
import org.lean.presentation.interaction.LeanInteractionParameter;
import org.lean.presentation.layout.LeanLayout;
import org.lean.presentation.page.LeanPage;
import org.lean.presentation.theme.LeanTheme;

import java.util.Arrays;
import java.util.List;

public class TestViewer {

  public static void main( String[] args ) {
    try {
      LeanEnvironment.init();
      HopEnvironment.init();
      Display display = new Display();
      SimpleLoggingObject loggingObject = new SimpleLoggingObject( "LPV", LoggingObjectType.GENERAL, null );

      IHopMetadataProvider metadataProvider = new MemoryMetadataProvider();
      IVariables variables = Variables.getADefaultVariableSpace();

      String dashboardName = "Dashboard";
      List<String> presentationNames = Arrays.asList( "Presentation A", "Presentation B", "Presentation C", "Presentation D", "Presentation E" );
      for ( String presentationName : presentationNames ) {
        generateLabelPresentation( metadataProvider, dashboardName, presentationName );
      }
      generateDashboard( metadataProvider, dashboardName, presentationNames );

      Shell shell = new Shell( display, SWT.APPLICATION_MODAL | SWT.CLOSE );
      shell.setLayout( new FillLayout() );

      // LeanPresentationViewer viewer =

      new LeanPresentationViewer( shell, loggingObject, variables, metadataProvider, dashboardName );

      shell.layout();
      shell.setSize( 794, 1123 ); ;
      shell.open();
      shell.addListener( SWT.Close, e -> display.dispose() );

      while ( !display.isDisposed() ) {
        if ( !display.readAndDispatch() ) {
          display.sleep();
        }
      }

      System.exit( 0 );
    } catch ( Exception e ) {
      System.err.println( "Error encountered: " + e.getMessage() );
      e.printStackTrace();
      System.exit( 1 );
    }
  }

  private static void generateDashboard( IHopMetadataProvider metadataProvider, String dashboardName, List<String> presentationNames ) throws HopException {
    // Add a simple presentation to the metadata
    //
    IHopMetadataSerializer<LeanPresentation> serializer = metadataProvider.getSerializer( LeanPresentation.class );

    LeanPresentation presentation = new LeanPresentation();
    presentation.setName( dashboardName );

    LeanPage page = new LeanPage( 1, 794, 1123, 25, 25, 25, 25 );
    presentation.getPages().add( page );

    // Add all the presentation names as labels...
    //
    String relativeTopComponent = null;

    for ( String presentationName : presentationNames ) {
      LeanLabelComponent leanLabelComponent = new LeanLabelComponent();
      leanLabelComponent.setLabel( presentationName );
      leanLabelComponent.setDefaultFont( new LeanFont( "Arial", "48", false, false ) );
      leanLabelComponent.setHorizontalAlignment( LeanHorizontalAlignment.LEFT );
      leanLabelComponent.setVerticalAlignment( LeanVerticalAlignment.BOTTOM );

      LeanComponent labelComponent = new LeanComponent( presentationName, leanLabelComponent );
      LeanLayout labelLayout = new LeanLayout();
      labelLayout.setLeft( new LeanAttachment( null, 0, 0, LeanAttachment.Alignment.CENTER ) );
      if ( relativeTopComponent == null ) {
        labelLayout.setTop( new LeanAttachment( null, 0, 100, LeanAttachment.Alignment.TOP ) );
      } else {
        labelLayout.setTop( new LeanAttachment( relativeTopComponent, 0, 30, LeanAttachment.Alignment.BOTTOM ) );
      }
      labelComponent.setLayout( labelLayout );
      labelComponent.setSize( null );

      // Let's add an interaction...
      // If a user double-clicks on the label we open the shown presentation...
      //
      LeanInteractionAction action = new LeanInteractionAction( LeanInteractionAction.ActionType.OpenPresentation, presentationName );
      action.getParameters().add( new LeanInteractionParameter( LeanInteractionParameter.ParameterSourceType.ItemValue, "SOURCE_VALUE" ) );
      action.getParameters().add( new LeanInteractionParameter( LeanInteractionParameter.ParameterSourceType.PresentationName, "SOURCE_PRESENTATION" ) );

      LeanInteraction interaction = new LeanInteraction(
        LeanInteractionMethod.SingleClick,
        new LeanInteractionLocation( presentationName, "LeanLabelComponent", null, null ),
        action
        );
      presentation.getInteractions().add( interaction );

      page.getComponents().add( labelComponent );

      relativeTopComponent = presentationName;
    }

    LeanTheme theme = LeanTheme.getDefault();
    presentation.getThemes().add( theme );
    presentation.setDefaultThemeName( theme.getName() );

    // Save it in the metadata
    //
    serializer.save( presentation );
  }

  private static void generateLabelPresentation( IHopMetadataProvider metadataProvider, String dashboardName, String presentationName ) throws HopException {
    // Add a simple presentation to the metadata
    //
    IHopMetadataSerializer<LeanPresentation> serializer = metadataProvider.getSerializer( LeanPresentation.class );

    LeanPresentation presentation = new LeanPresentation();
    presentation.setName( presentationName );

    LeanPage page = new LeanPage( 1, 794, 1123, 25, 25, 25, 25 );
    presentation.getPages().add( page );

    // A label in the center...
    //
    {
      LeanLabelComponent leanLabelComponent = new LeanLabelComponent();
      leanLabelComponent.setLabel( presentationName );
      leanLabelComponent.setDefaultFont( new LeanFont( "Arial", "64", true, false ) );

      LeanComponent labelComponent = new LeanComponent( "Label", leanLabelComponent );
      LeanLayout labelLayout = new LeanLayout();
      labelLayout.setLeft( new LeanAttachment( null, 0, 0, LeanAttachment.Alignment.CENTER ) );
      labelLayout.setTop( new LeanAttachment( null, 0, 0, LeanAttachment.Alignment.CENTER ) );
      labelComponent.setLayout( labelLayout );
      labelComponent.setSize( null );
      page.getComponents().add( labelComponent );
    }

    // Source value variable label below...
    //
    {
      LeanLabelComponent leanLabelComponent = new LeanLabelComponent();
      leanLabelComponent.setLabel( "Source value = ${SOURCE_VALUE}" );
      leanLabelComponent.setDefaultFont( new LeanFont( "Arial", "24", false, true ) );

      LeanComponent labelComponent = new LeanComponent( "SourceValue", leanLabelComponent );
      LeanLayout labelLayout = new LeanLayout();
      labelLayout.setLeft( new LeanAttachment( null, 0, 0, LeanAttachment.Alignment.CENTER ) );
      labelLayout.setTop( new LeanAttachment( "Label", 0, 50, LeanAttachment.Alignment.BOTTOM ) );
      labelComponent.setLayout( labelLayout );
      labelComponent.setSize( null );
      page.getComponents().add( labelComponent );
    }

    // Source presentation variable label below...
    //
    {
      LeanLabelComponent leanLabelComponent = new LeanLabelComponent();
      leanLabelComponent.setLabel( "Source presentation = ${SOURCE_PRESENTATION}" );
      leanLabelComponent.setDefaultFont( new LeanFont( "Arial", "24", false, true ) );

      LeanComponent labelComponent = new LeanComponent( "SourcePresentation", leanLabelComponent );
      LeanLayout labelLayout = new LeanLayout();
      labelLayout.setLeft( new LeanAttachment( null, 0, 0, LeanAttachment.Alignment.CENTER ) );
      labelLayout.setTop( new LeanAttachment( "SourceValue", 0, 50, LeanAttachment.Alignment.BOTTOM ) );
      labelComponent.setLayout( labelLayout );
      labelComponent.setSize( null );
      page.getComponents().add( labelComponent );
    }


    // A small link back to the dashboard at the bottom
    //
    LeanLabelComponent leanDashComponent = new LeanLabelComponent();
    leanDashComponent.setLabel( "Back to : " + dashboardName );
    leanDashComponent.setDefaultFont( new LeanFont( "Arial", "32", false, false ) );
    leanDashComponent.setHorizontalAlignment( LeanHorizontalAlignment.CENTER );
    leanDashComponent.setVerticalAlignment( LeanVerticalAlignment.MIDDLE );

    LeanComponent dashComponent = new LeanComponent( dashboardName, leanDashComponent );
    LeanLayout dashLayout = new LeanLayout();
    dashLayout.setRight( new LeanAttachment( null, 0, 0, LeanAttachment.Alignment.RIGHT ) );
    dashLayout.setBottom( new LeanAttachment( null, 0, 0 ) );
    dashComponent.setLayout( dashLayout );
    dashComponent.setSize( null );
    page.getComponents().add( dashComponent );

    // Let's add an interaction...
    // If a user double-clicks on the label we open the shown presentation...
    //
    LeanInteraction interaction = new LeanInteraction(
      new LeanInteractionMethod( true, false ), // double click
      new LeanInteractionLocation( dashboardName, "LeanLabelComponent", null, null ),
      new LeanInteractionAction( LeanInteractionAction.ActionType.OpenPresentation, dashboardName )
    );
    presentation.getInteractions().add( interaction );

    LeanTheme theme = LeanTheme.getDefault();
    presentation.getThemes().add( theme );
    presentation.setDefaultThemeName( theme.getName() );

    // Save it in the metadata
    //
    serializer.save( presentation );
  }

}
