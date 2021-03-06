/*******************************************************************************
 * Pentaho Data Science
 * <p/>
 * Copyright (c) 2002-2018 Hitachi Vantara. All rights reserved.
 * <p/>
 * ******************************************************************************
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/
 * <p/>
 ******************************************************************************/

package org.pentaho.di.ui.trans.steps.pmi;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.ui.core.PropsUI;
import org.pentaho.di.ui.core.widget.ComboVar;
import org.pentaho.di.ui.core.widget.TextVar;
import org.pentaho.di.ui.trans.step.BaseStepDialog;
import org.pentaho.pmi.Scheme;
import org.pentaho.pmi.SchemeUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.pentaho.di.core.Const.MARGIN;

/**
 * Implements an (incomplete) SWT version of Weka's GenericObjectEditor.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class GOEDialog extends Dialog {

  protected static final int FIRST_LABEL_RIGHT_PERCENTAGE = 35;
  protected static final int FIRST_PROMPT_RIGHT_PERCENTAGE = 60;
  // protected static final int SECOND_LABEL_RIGHT_PERCENTAGE = 70;
  protected static final int SECOND_PROMPT_RIGHT_PERCENTAGE = 70;
  protected static final int THIRD_PROMPT_RIGHT_PERCENTAGE = 80;

  protected PropsUI m_props;
  protected Shell m_parent;
  protected Shell m_shell;
  protected Map<String, Object> m_objectToEditMeta;
  protected Object m_objectToEdit;

  protected Map<String, Object> m_schemeWidgets = new LinkedHashMap<>();

  protected VariableSpace m_vars;

  protected Control lastControl;

  protected int m_returnValue;

  public GOEDialog( Shell shell, int i, Object objectToEdit, VariableSpace vars ) throws Exception {
    super( shell, i );

    m_parent = shell;
    m_objectToEdit = objectToEdit;
    m_objectToEditMeta =
        m_objectToEdit instanceof Scheme ? ( (Scheme) m_objectToEdit ).getSchemeInfo() :
            SchemeUtils.getSchemeParameters( m_objectToEdit );
    m_props = PropsUI.getInstance();
    m_vars = vars;
  }

  public int open() {
    Display display = m_parent.getDisplay();
    m_shell = new Shell( m_parent, 2160 );
    m_props.setLook( m_shell );
    FormLayout formLayout = new FormLayout();
    formLayout.marginWidth = 5;
    formLayout.marginHeight = 5;
    m_shell.setLayout( formLayout );
    String title = m_objectToEditMeta.get( "topLevelClass" ).toString();
    title = title.substring( title.lastIndexOf( '.' ) + 1 );
    m_shell.setText( title );
    int margin = 4;

    buildEditorSheet();

    m_shell.addShellListener( new ShellAdapter() {
      @Override public void shellClosed( ShellEvent shellEvent ) {
        cancel();
      }
    } );

    m_shell.layout();
    m_shell.pack();
    m_shell.open();

    while ( !m_shell.isDisposed() ) {
      if ( !display.readAndDispatch() ) {
        display.sleep();
      }
    }

    return m_returnValue;
  }

  private void cancel() {
    m_returnValue = SWT.CANCEL;
    this.dispose();
  }

  private void ok() {
    widgetValuesToPropsMap( m_objectToEdit, m_objectToEditMeta, m_schemeWidgets );
    m_returnValue = SWT.OK;
    this.dispose();
  }

  @SuppressWarnings( "unchecked" )
  public static void widgetValuesToPropsMap( Object objectToEdit, Map<String, Object> objectToEditMeta,
      Map<String, Object> schemeWidgets ) {
    Map<String, Map<String, Object>>
        properties =
        (Map<String, Map<String, Object>>) objectToEditMeta.get( "properties" );

    for ( Map.Entry<String, Object> e : schemeWidgets.entrySet() ) {
      String propName = e.getKey();
      Object widget = e.getValue();
      String widgetVal = "";

      if ( widget instanceof ComboVar ) {
        widgetVal = ( (ComboVar) widget ).getText();
      } else if ( widget instanceof TextVar ) {
        widgetVal = ( (TextVar) widget ).getText();
      } else if ( widget instanceof Button ) {
        widgetVal = ( (Button) widget ).getSelection() ? "true" : "false";
      }

      Map<String, Object> propDetails = properties.get( propName );
      if ( propDetails != null ) {
        propDetails.put( "value", widgetVal );
      }
    }

    // now set the values on the underlying object
    try {
      if ( objectToEdit instanceof Scheme ) {
        ( (Scheme) objectToEdit ).setSchemeParameters( properties );
      } else {
        SchemeUtils.setSchemeParameters( objectToEdit, properties );
      }
    } catch ( Exception e ) {
      e.printStackTrace();
      // TODO popup error dialog
    }
  }

  @SuppressWarnings( "unchecked" ) protected void buildEditorSheet() {

    lastControl = null;

    String helpInfo = (String) m_objectToEditMeta.get( "helpSummary" );
    String helpSynopsis = (String) m_objectToEditMeta.get( "helpSynopsis" );
    if ( !Const.isEmpty( helpInfo ) ) {
      Group helpGroup = new Group( m_shell, SWT.SHADOW_NONE );
      m_props.setLook( helpGroup );
      helpGroup.setText( "About" );
      FormLayout fl = new FormLayout();
      fl.marginWidth = 10;
      fl.marginHeight = 10;
      helpGroup.setLayout( fl );
      FormData fd = new FormData();
      fd.left = new FormAttachment( 0, 0 );
      fd.right = new FormAttachment( 100, 0 );
      fd.top = new FormAttachment( 0, 0 );
      helpGroup.setLayoutData( fd );

      // TODO do this properly at some stage
      Button moreButton = null;
      /* if ( !Const.isEmpty( helpSynopsis ) ) {
        moreButton = new Button( helpGroup, SWT.PUSH );
        m_props.setLook( moreButton );
        moreButton.setText( "More..." );
        fd = new FormData();
        fd.top = new FormAttachment( 0, 4 );
        fd.right = new FormAttachment( 100, -4 );
        moreButton.setLayoutData( fd );

        moreButton.addSelectionListener( new SelectionAdapter() {
          @Override public void widgetSelected( SelectionEvent selectionEvent ) {
            // TODO popup "more" window
          }
        } );
      } */

      Label aboutLab = new Label( helpGroup, SWT.LEFT );
      m_props.setLook( aboutLab );
      aboutLab.setText( helpInfo );
      fd = new FormData();
      fd.top = new FormAttachment( 0, 4 );
      fd.left = new FormAttachment( 0, 0 );
      fd.right = moreButton != null ? new FormAttachment( moreButton, -4 ) : new FormAttachment( 100, -4 );
      aboutLab.setLayoutData( fd );
      lastControl = helpGroup;
    }

    final Map<String, Map<String, Object>>
        properties =
        (Map<String, Map<String, Object>>) m_objectToEditMeta.get( "properties" );

    for ( Map.Entry<String, Map<String, Object>> e : properties.entrySet() ) {
      final String propName = e.getKey();
      final Map<String, Object> propDetails = e.getValue();
      String tipText = (String) propDetails.get( "tip-text" );
      final String type = (String) propDetails.get( "type" );
      String propLabelText = (String) propDetails.get( "label" );
      Object value = propDetails.get( "value" );

      final Label propLabel = new Label( m_shell, SWT.RIGHT );
      m_props.setLook( propLabel );
      propLabel.setText( propLabelText );
      if ( !Const.isEmpty( tipText ) ) {
        propLabel.setToolTipText( tipText );
      }
      propLabel.setLayoutData( getFirstLabelFormData() );

      if ( type.equalsIgnoreCase( "object" ) ) {
        String objectTextRep = value.toString();
        Object objectValue = propDetails.get( "objectValue" );
        final String goeBaseType = propDetails.get( "goeBaseType" ).toString();
        /* if ( objectValue != null ) {
          objectTextRep =
              ( objectValue instanceof OptionHandler ) ?
                  Utils.joinOptions( ( (OptionHandler) objectValue ).getOptions() ) :
                  objectValue.getClass().getCanonicalName();
        } */
        final Label objectValueLab = new Label( m_shell, SWT.RIGHT );
        m_props.setLook( objectValueLab );
        objectValueLab.setText( objectTextRep );
        objectValueLab.setLayoutData( getFirstPromptFormData( propLabel ) );

        final Button objectValEditBut = new Button( m_shell, SWT.PUSH );
        m_props.setLook( objectValEditBut );
        objectValEditBut.setText( "Edit..." /*+ objectTextRep */ );
        objectValEditBut.setLayoutData( getSecondPromptFormData( objectValueLab ) );

        final Button objectChooseBut = new Button( m_shell, SWT.PUSH );
        m_props.setLook( objectChooseBut );
        objectChooseBut.setText( "Choose..." );
        objectChooseBut.setLayoutData( getThirdPropmtFormData( objectValEditBut ) );
        objectChooseBut.addSelectionListener( new SelectionAdapter() {
          @Override public void widgetSelected( SelectionEvent selectionEvent ) {
            super.widgetSelected( selectionEvent );
            Object selectedObject = null;
            try {
              objectChooseBut.setEnabled( false );
              objectValEditBut.setEnabled( false );
              GOETree treeDialog = new GOETree( getParent(), SWT.OK | SWT.CANCEL, goeBaseType );
              int result = treeDialog.open();
              if ( result == SWT.OK ) {
                Object selectedTreeValue = treeDialog.getSelectedTreeObject();
                if ( selectedTreeValue != null ) {
                  Map<String, Object> propDetails = properties.get( propName );
                  if ( propDetails != null ) {
                    propDetails.put( "objectValue", selectedTreeValue );
                  }
                }
                objectValueLab.setText( SchemeUtils.getTextRepresentationOfObjectValue( selectedTreeValue ) );
              }
            } catch ( Exception ex ) {
              // TODO popup error dialog
              ex.printStackTrace();
            } finally {
              objectChooseBut.setEnabled( true );
              objectValEditBut.setEnabled( true );
            }
          }
        } );

        objectValEditBut.addSelectionListener( new SelectionAdapter() {
          @Override public void widgetSelected( SelectionEvent selectionEvent ) {
            super.widgetSelected( selectionEvent );
            objectValEditBut.setEnabled( false );
            objectChooseBut.setEnabled( false );
            try {
              GOEDialog
                  dialog =
                  new GOEDialog( GOEDialog.this.getParent(), SWT.OK | SWT.CANCEL, propDetails.get( "objectValue" ),
                      m_vars );
              dialog.open();

              objectValueLab
                  .setText( SchemeUtils.getTextRepresentationOfObjectValue( propDetails.get( "objectValue" ) ) );
            } catch ( Exception e1 ) {
              e1.printStackTrace();
            } finally {
              objectValEditBut.setEnabled( true );
              objectChooseBut.setEnabled( true );
            }
          }
        } );

        lastControl = objectValEditBut;
      } else if ( type.equalsIgnoreCase( "array" ) ) {
        // TODO
      } else if ( type.equalsIgnoreCase( "pick-list" ) ) {
        String pickListValues = (String) propDetails.get( "pick-list-values" );
        String[] vals = pickListValues.split( "," );
        ComboVar pickListCombo = new ComboVar( m_vars, m_shell, SWT.BORDER | SWT.READ_ONLY );
        m_props.setLook( pickListCombo );
        for ( String v : vals ) {
          pickListCombo.add( v.trim() );
        }
        if ( value != null && value.toString().length() > 0 ) {
          pickListCombo.setText( value.toString() );
        }
        pickListCombo.addSelectionListener( new SelectionAdapter() {
          @Override public void widgetSelected( SelectionEvent selectionEvent ) {
            super.widgetSelected( selectionEvent );
            // TODO (main dialog should set changed based on button clicked in this dialog)
            // m_inputMeta.setChanged();
          }
        } );
        pickListCombo.setLayoutData( getFirstPromptFormData( propLabel ) );
        lastControl = pickListCombo;
        m_schemeWidgets.put( propName, pickListCombo );
      } else if ( type.equalsIgnoreCase( "boolean" ) ) {
        Button boolBut = new Button( m_shell, SWT.CHECK );
        m_props.setLook( boolBut );
        boolBut.setLayoutData( getFirstPromptFormData( propLabel ) );
        if ( value != null && value.toString().length() > 0 ) {
          boolBut.setSelection( Boolean.parseBoolean( value.toString() ) );
        }
        lastControl = boolBut;
        m_schemeWidgets.put( propName, boolBut );
      } else {
        TextVar propVar = new TextVar( m_vars, m_shell, SWT.SINGLE | SWT.LEAD | SWT.BORDER );
        m_props.setLook( propVar );
        if ( value != null ) {
          propVar.setText( value.toString() );
        }
        // propVar.addModifyListener( m_simpleModifyListener );
        propVar.setLayoutData( getFirstPromptFormData( propLabel ) );
        lastControl = propVar;
        m_schemeWidgets.put( propName, propVar );
      }
    }

    // add buttons
    List<Button> buttons = new ArrayList<>();
    Button okBut = new Button( m_shell, SWT.PUSH );
    okBut.setText( "OK" );
    m_props.setLook( okBut );
    okBut.addSelectionListener( new SelectionAdapter() {
      @Override public void widgetSelected( SelectionEvent selectionEvent ) {
        ok();
      }
    } );
    buttons.add( okBut );

    Button cancelBut = new Button( m_shell, SWT.PUSH );
    cancelBut.setText( "Cancel" );
    m_props.setLook( cancelBut );
    cancelBut.addSelectionListener( new SelectionAdapter() {
      @Override public void widgetSelected( SelectionEvent selectionEvent ) {
        cancel();
      }
    } );
    buttons.add( cancelBut );

    BaseStepDialog.positionBottomButtons( m_shell, buttons.toArray( new Button[buttons.size()] ), 4, lastControl );
  }

  public void dispose() {
    m_shell.dispose();
  }

  private FormData getFirstLabelFormData() {
    FormData fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.right = new FormAttachment( FIRST_LABEL_RIGHT_PERCENTAGE, 0 );
    fd.top = new FormAttachment( lastControl, MARGIN );
    return fd;
  }

  private FormData getFirstPromptFormData( Control prevControl ) {
    FormData fd = new FormData();
    fd.left = new FormAttachment( prevControl, MARGIN );
    fd.right = new FormAttachment( FIRST_PROMPT_RIGHT_PERCENTAGE, 0 );
    fd.top = new FormAttachment( lastControl, MARGIN );
    return fd;
  }

  private FormData getSecondPromptFormData( Control prevControl ) {
    FormData fd = new FormData();
    fd.left = new FormAttachment( prevControl, MARGIN );
    fd.top = new FormAttachment( lastControl, MARGIN );
    fd.right = new FormAttachment( SECOND_PROMPT_RIGHT_PERCENTAGE, 0 );
    return fd;
  }

  private FormData getThirdPropmtFormData( Control prevControl ) {
    FormData fd = new FormData();
    fd.left = new FormAttachment( prevControl, 0 );
    fd.right = new FormAttachment( THIRD_PROMPT_RIGHT_PERCENTAGE, 0 );
    fd.top = new FormAttachment( lastControl, MARGIN );

    return fd;
  }
}
