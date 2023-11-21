package org.phoebus.service.saveandrestore.epics;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.epics.pva.client.PVAClient;
import org.epics.vtype.VBoolean;
import org.epics.vtype.VBooleanArray;
import org.epics.vtype.VByteArray;
import org.epics.vtype.VDoubleArray;
import org.epics.vtype.VEnum;
import org.epics.vtype.VEnumArray;
import org.epics.vtype.VFloatArray;
import org.epics.vtype.VIntArray;
import org.epics.vtype.VLongArray;
import org.epics.vtype.VNumber;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VShortArray;
import org.epics.vtype.VString;
import org.epics.vtype.VStringArray;
import org.epics.vtype.VType;
import org.epics.vtype.VUByteArray;
import org.epics.vtype.VUIntArray;
import org.epics.vtype.VULongArray;
import org.epics.vtype.VUShortArray;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.core.vtypes.VTypeHelper;
import org.phoebus.service.saveandrestore.epics.RestoreResult;
public class SnapshotRestorer {

   PVAClient pva;
   private final Logger LOG = Logger.getLogger(SnapshotRestorer.class.getName());
   private long timeoutMillis = 5000;

   public SnapshotRestorer() throws Exception {
      pva = new PVAClient();
   }

   /** Restore PV values from a list of snapshot items
    *
    *  <p> Writes concurrently (with timeout) the pv value to the non null set PVs in the snapshot items.
           Uses synchonized to ensure only one frontend can write at a time.
           Returns a list of the snapshot items you have set, with an error message if an
           error occurred.
    *
    *  @param snapshotItems {@link SnapshotItem}
    */
   public synchronized List<RestoreResult> restorePVValues(List<SnapshotItem> snapshotItems) {

      var futures = snapshotItems.stream().filter(
            (snapshot_item) -> snapshot_item.getConfigPv().getPvName() != null)
            .map((snapshotItem) -> {
               var pvName = snapshotItem.getConfigPv().getPvName();
               var pvValue = snapshotItem.getValue();
               var channel = pva.getChannel(pvName);
               CompletableFuture<Boolean> connected = channel.connect().completeOnTimeout(false, timeoutMillis, TimeUnit.MILLISECONDS);
               CompletableFuture<RestoreResult> writeFuture = connected.thenComposeAsync(
                     (isConnected) -> {
                        String error;
                        try {
                           if (isConnected) {
                              Object rawValue = vTypeToObject(pvValue);
                              channel.write(true, "value", rawValue).get(timeoutMillis, TimeUnit.MILLISECONDS);
                              error = null;
                           } else {
                              LOG.warning(String.format("Tried to set %s but the channel is disconnnected", pvName));
                              error = "PV disconnected";
                           }
                        } catch (Exception e) {
                           error = e.getMessage();
                           LOG.warning(String.format("Error setting PV %s", error));
                        } finally {
                           channel.close();
                        }

                        var restoreResult = new RestoreResult();
                        restoreResult.setSnapshotItem(snapshotItem);
                        restoreResult.setErrorMsg(error);
                        return CompletableFuture.completedFuture(restoreResult);
                     });
               return writeFuture;
            })
            .collect(Collectors.toList());

      CompletableFuture<Void> all_done = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

      // Wait on the futures concurrently
      all_done.join();

      // Joins should not block as all the futures should be completed.
      return futures.stream().map(
         (future) -> future.join()
      ).collect(Collectors.toList());
   }

    /**
    * Convert a vType to its Object representation
    *  @param type {@link VType}
   */
   private Object vTypeToObject(VType type) {
        if (type == null) {
            return null;
        }
        if (type instanceof VNumberArray) {
            if (type instanceof VIntArray || type instanceof VUIntArray) {
                return VTypeHelper.toIntegers(type);
            } else if (type instanceof VDoubleArray) {
                return VTypeHelper.toDoubles(type);
            } else if (type instanceof VFloatArray) {
                return VTypeHelper.toFloats(type);
            } else if (type instanceof VLongArray || type instanceof VULongArray) {
                return VTypeHelper.toLongs(type);
            } else if (type instanceof VShortArray || type instanceof VUShortArray) {
                return VTypeHelper.toShorts(type);
            } else if (type instanceof VByteArray || type instanceof VUByteArray) {
                return VTypeHelper.toBytes(type);
            }
        } else if (type instanceof VEnumArray) {
            List<String> data = ((VEnumArray) type).getData();
            return data.toArray(new String[data.size()]);
        } else if (type instanceof VStringArray) {
            List<String> data = ((VStringArray) type).getData();
            return data.toArray(new String[data.size()]);
        } else if (type instanceof VBooleanArray) {
            return VTypeHelper.toBooleans(type);
        } else if (type instanceof VNumber) {
            return ((VNumber) type).getValue();
        } else if (type instanceof VEnum) {
            return ((VEnum) type).getIndex();
        } else if (type instanceof VString) {
            return ((VString) type).getValue();
        } else if (type instanceof VBoolean) {
            return ((VBoolean) type).getValue();
        }
        return null;
   }
}
