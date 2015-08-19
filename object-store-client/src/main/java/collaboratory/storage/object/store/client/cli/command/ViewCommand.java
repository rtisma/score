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
package collaboratory.storage.object.store.client.cli.command;

import java.net.URL;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import collaboratory.storage.object.store.client.download.ObjectDownload;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.val;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.seekablestream.SeekableHTTPStream;

/**
 * Resolves URL for a supplied object id.
 */
@Component
@Parameters(separators = "=", commandDescription = "object to resolve URL for")
public class ViewCommand extends AbstractClientCommand {

  @Parameter(names = "--object-id", description = "object id to resolve URL for", required = true)
  private String oid;

  @Parameter(names = "--location", description = "genomic location", required = true)
  private String location;

  @Autowired
  private ObjectDownload downloader;

  @Override
  @SneakyThrows
  public int execute() {
    val entity = findEntity(oid);
    val fileName = entity.get("fileName").textValue();
    if (!fileName.endsWith(".bam")) {
      println("Cannot view non-BAM files");
      return FAILURE_STATUS;
    }

    val gnosId = entity.get("gnosId").textValue();
    val entities = findEntitiesByGnosId(gnosId);
    val indexFileId = resolveIndexFileId(entities);
    if (!indexFileId.isPresent()) {
      println("Cannot find index file");
      return FAILURE_STATUS;
    }

    val bamFileUrl = downloader.getUrl(oid);
    val indexFileUrl = downloader.getUrl(indexFileId.get());

    @Cleanup
    val reader = new SAMFileReader(
        new SeekableHTTPStream(bamFileUrl),
        new SeekableHTTPStream(indexFileUrl),
        false);

    val iterator = reader.query("3", 1000, 2000, true);
    while (iterator.hasNext()) {
      val record = iterator.next();
      println(record.toString());
    }

    return SUCCESS_STATUS;
  }

  private Optional<String> resolveIndexFileId(final com.fasterxml.jackson.databind.node.ObjectNode entities) {
    for (val entity : entities.withArray("content")) {
      val fileName = entity.get("fileName").textValue();
      if (fileName.endsWith(".bai")) {
        return Optional.of(entity.get("id").textValue());
      }
    }

    return Optional.empty();
  }

  @SneakyThrows
  private ObjectNode findEntity(String objectId) {
    return read("/" + objectId);
  }

  @SneakyThrows
  private ObjectNode findEntitiesByGnosId(String gnosId) {
    return read("?gnosId=" + gnosId);
  }

  @SneakyThrows
  private ObjectNode read(String url) {
    return new ObjectMapper().readValue(new URL("https://meta.icgc.org/entities" + url), ObjectNode.class);
  }

}
