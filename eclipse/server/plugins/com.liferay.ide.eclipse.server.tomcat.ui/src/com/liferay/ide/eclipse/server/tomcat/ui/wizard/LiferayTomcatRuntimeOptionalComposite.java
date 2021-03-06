/*******************************************************************************
 * Copyright (c) 2000-2012 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 *******************************************************************************/

package com.liferay.ide.eclipse.server.tomcat.ui.wizard;

import static com.liferay.ide.eclipse.core.util.CoreUtil.empty;

import com.liferay.ide.eclipse.server.tomcat.core.ILiferayTomcatRuntime;
import com.liferay.ide.eclipse.server.tomcat.core.util.LiferayTomcatUtil;
import com.liferay.ide.eclipse.server.ui.LiferayServerUIPlugin;
import com.liferay.ide.eclipse.ui.util.SWTUtil;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jst.server.core.IJavaRuntime;
import org.eclipse.jst.server.tomcat.core.internal.ITomcatRuntimeWorkingCopy;
import org.eclipse.jst.server.tomcat.ui.internal.TomcatRuntimeComposite;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;

/**
 * @author Greg Amerson
 */
@SuppressWarnings( { "restriction" } )
public class LiferayTomcatRuntimeOptionalComposite extends TomcatRuntimeComposite implements ModifyListener
{

	public static void setFieldValue( Text field, String value )
	{
		if ( field != null && !field.isDisposed() )
		{
			field.setText( value != null ? value : "" );
		}
	}

	protected Text bundleZipField;

	protected boolean ignoreModifyEvent;

	private Text javadocField;

	public LiferayTomcatRuntimeOptionalComposite( Composite parent, IWizardHandle wizard )
	{
		super( parent, wizard );

		wizard.setTitle( "Liferay Runtime Tomcat Bundle" );
		wizard.setDescription( "Specify extra settings for Liferay Tomcat bundle." );
		wizard.setImageDescriptor( LiferayServerUIPlugin.getImageDescriptor( LiferayServerUIPlugin.IMG_WIZ_RUNTIME ) );
	}

	protected Button createButton( String text, int style )
	{
		Button button = new Button( this, style );
		button.setText( text );

		GridDataFactory.generate( button, 2, 1 );

		return button;
	}

	@Override
	protected void createControl()
	{
		setLayout( createLayout() );
		setLayoutData( new GridData( GridData.FILL_BOTH ) );

		createFields();

		init();

		validate();

		Dialog.applyDialogFont( this );
	}

	protected void createFields()
	{
		javadocField = createTextField( "Liferay Javadoc URL (zip file, local directory, or online url" );
		javadocField.addModifyListener( this );

		SWTUtil.createButton( this, "Browse zip..." ).addSelectionListener( new SelectionAdapter()
		{
			public void widgetSelected( SelectionEvent e )
			{
				FileDialog fd = new FileDialog( LiferayTomcatRuntimeOptionalComposite.this.getShell() );

				fd.setText( "Select Liferay Javadoc zip file." );

				String selectedFile = fd.open();

				if ( selectedFile != null )
				{
					String javadocZipURL = getJavadocZipURL( selectedFile );

					if ( javadocZipURL != null )
					{
						javadocField.setText( javadocZipURL );
					}
					else
					{
						MessageDialog.openInformation(
							LiferayTomcatRuntimeOptionalComposite.this.getShell(), "Liferay Tomcat Runtime",
							"Selected file is not a valid Liferay Javadoc zip file." );
					}
				}
			}
		} );

		SWTUtil.createLabel( this, "", 1 );

		SWTUtil.createButton( this, "Browse directory..." ).addSelectionListener( new SelectionAdapter()
		{
			public void widgetSelected( SelectionEvent e )
			{
				DirectoryDialog dd = new DirectoryDialog( LiferayTomcatRuntimeOptionalComposite.this.getShell() );

				dd.setText( "Select Liferay Javadoc directory." );
				dd.setFilterPath( javadocField.getText() );

				String selectedFile = dd.open();

				if ( selectedFile != null )
				{
					String javadocDirectoryURL = getJavadocDirectoryURL( selectedFile );

					if ( javadocDirectoryURL != null )
					{
						javadocField.setText( javadocDirectoryURL );
					}
					else
					{
						MessageDialog.openInformation(
							LiferayTomcatRuntimeOptionalComposite.this.getShell(), "Liferay Tomcat Runtime",
							"Selected directory is not a valid Liferay Javadoc directory location." );
					}
				}
			}
		} );

		bundleZipField = createTextField( "Liferay Tomcat bundle zip file (required for Ext plugins)" );
		bundleZipField.addModifyListener( this );

		SWTUtil.createButton( this, "Browse..." ).addSelectionListener( new SelectionAdapter()
		{
			public void widgetSelected( SelectionEvent e )
			{
				FileDialog fd = new FileDialog( LiferayTomcatRuntimeOptionalComposite.this.getShell() );

				fd.setText( "Select Liferay Tomcat bundle zip file" );
				fd.setFilterPath( bundleZipField.getText() );

				String selectedFile = fd.open();

				if ( selectedFile != null )
				{
					bundleZipField.setText( selectedFile );
				}
			}
		} );
	}

	protected Label createLabel( String text )
	{
		Label label = new Label( this, SWT.NONE );
		label.setText( text );

		GridDataFactory.generate( label, 2, 1 );

		return label;
	}

	protected Layout createLayout()
	{
		GridLayout layout = new GridLayout( 2, false );
		return layout;
	}

	protected void createSpacer()
	{
		new Label( this, SWT.NONE );
	}

	protected Text createTextField( String labelText )
	{
		createLabel( labelText );

		Text text = new Text( this, SWT.BORDER );
		text.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );

		return text;
	}

	protected String getJavadocDirectoryURL( String selectedFile )
	{
		String retval = null;

		File javadocDirectory = new File( selectedFile );

		if ( javadocDirectory.exists() && javadocDirectory.isDirectory() )
		{
			// check one layer down
			File[] files = javadocDirectory.listFiles();

			if ( !empty( files ) )
			{
				for ( File nestedFile : files )
				{
					if ( nestedFile.getName().equals( "javadocs" ) )
					{
						javadocDirectory = nestedFile;
					}
				}

				for ( File nestedFile : files )
				{
					if ( nestedFile.getName().equals( "javadocs-all" ) )
					{
						javadocDirectory = nestedFile;
					}
				}

				File liferayDir = new File( javadocDirectory, "com/liferay" );

				if ( liferayDir.exists() )
				{
					try
					{
						retval = javadocDirectory.toURI().toURL().toExternalForm();
					}
					catch ( MalformedURLException e )
					{
					}
				}
			}
		}

		return retval;
	}

	protected String getJavadocZipURL( String selectedFile )
	{
		String retval = null;

		try
		{
			String rootEntryName = null;
			ZipEntry javadocEntry = null;

			final File javadocFile = new File( selectedFile );
			final ZipFile zipFile = new ZipFile( javadocFile );

			Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();

			ZipEntry rootEntry = zipEntries.nextElement();
			rootEntryName = new Path( rootEntry.getName() ).segment( 0 );

			if ( rootEntryName.endsWith( "/" ) )
			{
				rootEntryName = rootEntryName.substring( 0, rootEntryName.length() - 1 );
			}

			ZipEntry entry = zipEntries.nextElement();

			while ( entry != null && javadocEntry == null )
			{
				String entryName = entry.getName();

				if ( entryName.startsWith( rootEntryName + "/javadocs" ) )
				{
					ZipEntry allEntry = new ZipEntry( rootEntryName + "/javadocs-all" );

					if ( zipFile.getInputStream( allEntry ) != null )
					{
						javadocEntry = allEntry;
					}
					else
					{
						javadocEntry = entry;
					}
				}

				entry = zipEntries.nextElement();
			}

			if ( javadocEntry != null )
			{
				retval = "jar:" + javadocFile.toURI().toURL().toExternalForm() + "!/" + javadocEntry.getName();
			}
		}
		catch ( Exception e )
		{
			// we couldn't find value zip url for whatever reason so just return
		}

		return retval;
	}

	protected IJavaRuntime getJavaRuntime()
	{
		return (IJavaRuntime) this.runtime;
	}

	protected ILiferayTomcatRuntime getLiferayTomcatRuntime()
	{
		return LiferayTomcatUtil.getLiferayTomcatRuntime( this.runtimeWC );
	}

	protected IRuntimeWorkingCopy getRuntime()
	{
		return this.runtimeWC;
	}

	@Override
	protected void init()
	{
		if ( ( bundleZipField == null ) || getRuntime() == null )
		{
			return;
		}

		IPath bundleZipLocation = getLiferayTomcatRuntime().getBundleZipLocation();
		setFieldValue( bundleZipField, bundleZipLocation != null ? bundleZipLocation.toOSString() : "" );

		String javadocURL = getLiferayTomcatRuntime().getJavadocURL();
		setFieldValue( javadocField, javadocURL != null ? javadocURL : "" );
	}

	public void modifyText( ModifyEvent e )
	{
		if ( ignoreModifyEvent )
		{
			ignoreModifyEvent = false;
			return;
		}

		if ( e.getSource().equals( bundleZipField ) )
		{
			getLiferayTomcatRuntime().setBundleZipLocation( new Path( bundleZipField.getText() ) );
		}
		else if ( e.getSource().equals( javadocField ) )
		{
            String newJavadocURL = null;
            
            // if a file directory see if we need to correct
			String javadocValue = javadocField.getText();
			
		    try
			{
				URL javadocURL = new URL(javadocValue);
                
				if ( javadocURL.getProtocol() != null && javadocURL.getProtocol().startsWith( "http" ) )
				{
                    newJavadocURL = javadocValue;
			    }

				if ( newJavadocURL == null )
				{
					File javadocFile = new File( javadocValue );

					if ( javadocFile.isFile() )
					{
						newJavadocURL = javadocFile.toURI().toURL().toExternalForm();
					}
					else if ( javadocFile.isDirectory() )
					{

					}
					else
					{
						newJavadocURL = javadocValue;
					}
				}
			}
			catch ( MalformedURLException e1 )
			{
				newJavadocURL = javadocValue;
			}
			
			getLiferayTomcatRuntime().setJavadocURL( newJavadocURL );
		}

		validate();
	}

	@Override
	public void setRuntime( IRuntimeWorkingCopy newRuntime )
	{
		if ( newRuntime == null )
		{
			runtimeWC = null;
			runtime = null;
		}
		else
		{
			runtimeWC = newRuntime;
			runtime = (ITomcatRuntimeWorkingCopy) newRuntime.loadAdapter( ITomcatRuntimeWorkingCopy.class, null );
		}

		init();
		validate();
	}

}
