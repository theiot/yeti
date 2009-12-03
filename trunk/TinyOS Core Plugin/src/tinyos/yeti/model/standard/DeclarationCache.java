/*
 * Yeti 2, NesC development in Eclipse.
 * Copyright (C) 2009 ETH Zurich
 *
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Web:  http://tos-ide.ethz.ch
 * Mail: tos-ide@tik.ee.ethz.ch
 */
package tinyos.yeti.model.standard;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import tinyos.yeti.ProjectTOS;
import tinyos.yeti.ep.IParseFile;
import tinyos.yeti.ep.parser.IDeclaration;
import tinyos.yeti.ep.parser.IDeclarationFactory;
import tinyos.yeti.model.ProjectModel;

public class DeclarationCache extends StandardFileCache<IDeclaration[]>{
    private IDeclarationFactory factory;
    
    public DeclarationCache( ProjectModel model, IDeclarationFactory factory, String extension ){
        super( model, extension );
        this.factory = factory;
    }

    public IDeclaration[] readCache( IParseFile file, IProgressMonitor monitor ) throws IOException, CoreException{
        IFile cache = getCacheFile( file );
        DataInputStream in = new DataInputStream( new BufferedInputStream( cache.getContents() ));
        
        int modelVersion = in.readInt();
        if( modelVersion != 0 ){
            throw new IOException( "version of file is not valid" );
        }
        
        int version = in.readInt();
        int size = in.readInt();
        
        ProjectTOS project = model.getProject();
        
        monitor.beginTask( "Read cache of '" + file.getName() + "'", size );
        IDeclaration[] result = new IDeclaration[ size ];
        
        for( int i = 0; i < size; i++ ){
            result[i] = factory.read( version, in, project );
            monitor.worked( 1 );
            if( monitor.isCanceled() ){
                monitor.done();
                in.close();
                return null;
            }
        }
        
        in.close();
        monitor.done();
        return result;
    }
    
    public void writeCache( IParseFile file, IDeclaration[] declarations, IProgressMonitor monitor ) throws IOException, CoreException{
        IFile cache = getCacheFile( file );
        
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream( bytes );
        
        int version = factory.getVersion();
        int size = declarations == null ? 0 : declarations.length;
        
        out.writeInt( 0 );
        out.writeInt( version );
        out.writeInt( size );
        
        int ticks = 2*size;
        monitor.beginTask( "Write cache for '" + file.getName() + "'", ticks );
        
        ProjectTOS project = model.getProject();
        
        for( int i = 0; i < size; i++ ){
            factory.write( declarations[i], out, project );
            monitor.worked( 1 );
            ticks--;
            if( monitor.isCanceled() ){
                monitor.done();
                out.close();
                cache.delete( true, new SubProgressMonitor( monitor, ticks ) );
                return;
            }
        }
        
        out.close();
        ByteArrayInputStream input = new ByteArrayInputStream( bytes.toByteArray() );
        
        if( cache.exists() ){
            cache.delete( true, new SubProgressMonitor( monitor, ticks/2 ) );
            ticks -= ticks/2;
        }
        
        if( monitor.isCanceled() ){
            monitor.done();
            return;
        }
     
        create( cache, input, new SubProgressMonitor( monitor, ticks ));
        monitor.done();
    }
}
