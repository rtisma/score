/*
 * Copyright (c) 2015 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package collaboratory.storage.object.store.client.upload;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

import lombok.AllArgsConstructor;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;

@AllArgsConstructor
public class FileInputChannel extends AbstractInputChannel {

  private final File file;
  private final long offset;
  private final long length;
  private String md5;

  @Override
  public void reset() throws IOException {
  }

  @Override
  public void writeTo(OutputStream os) throws IOException {
    try (FileInputStream is = new FileInputStream(file)) {
      HashingOutputStream hos = new HashingOutputStream(Hashing.md5(), os);
      WritableByteChannel toChannel = Channels.newChannel(hos);
      is.getChannel().transferTo(offset, length, toChannel);
      toChannel.close();
      md5 = hos.hash().toString();
    }
  }

  @Override
  public long getlength() {
    return length;
  }

  @Override
  public String getMd5() {
    return md5;
  }

  @Override
  public void close() {
    // TODO Auto-generated method stub
  }

}
