/*
 * Copyright (C) 2018 European Spallation Source ERIC.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package db.migration.common;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;

/**
 * Migrates PV data from config_pv to pv table.
 * @author georgweiss
 * Created 24 Jan 2020
 */
public class V18_0__migrate_pv_data_step2 extends BaseJavaMigration{

    private static final int NO_ID = -1;

    @Override
    public void migrate(Context context) throws Exception {

        try (Statement statement = context.getConnection().createStatement()) {
            try (ResultSet rows = statement.executeQuery("SELECT * FROM config_pv")) {
                while (rows.next()) {
                    int id = rows.getInt(1);
                    String pvName = purifyPvName(rows.getString(2));
                    String readbackPvName = purifyPvName(rows.getString(3));
                    migratePv(context, pvName, readbackPvName, id);
                }
            }
        }
    }

    private void migratePv(Context context, String pvName, String readbackPvName, int configId) throws Exception{
        int pvId = getPvId(context, pvName);
        Timestamp time = new Timestamp(System.currentTimeMillis());
        Statement statement = context.getConnection().createStatement();
        if(pvId == NO_ID){
            String sql = "INSERT INTO pv (name, created, last_modified) VALUES('" + pvName + "', '" + time.toString() + "', '" + time.toString() + "')";
            statement.execute(sql);
            pvId = getPvId(context, pvName);
        }
        statement.execute("UPDATE config_pv SET pv_id=" + pvId + " WHERE id=" + configId);
        if(readbackPvName != null){
            pvId = getPvId(context, readbackPvName);
            if(pvId == NO_ID){
                String sql = "INSERT INTO pv (name, created, last_modified) VALUES('" + readbackPvName + "', '" + time.toString() + "', '" + time.toString() + "')";
                statement.execute(sql);
                pvId = getPvId(context, readbackPvName);
            }
            statement.execute("UPDATE config_pv SET readback_pv_id=" + pvId + " WHERE id=" + configId);
        }
        statement.close();
    }

    private int getPvId(Context context, String pvName) throws Exception{
        Statement statement = context.getConnection().createStatement();
        ResultSet rows = statement.executeQuery("SELECT id FROM pv WHERE name='" + pvName + "'");
        int pvId = rows.next() ? rows.getInt(1) : NO_ID;
        statement.close();
        return  pvId;
    }

    private String purifyPvName(String pvName){
        if(pvName == null){
            return null;
        }
        if(pvName.startsWith("ca://")){
            return pvName.substring("ca://".length());
        }
        if(pvName.startsWith("pva://")){
            return pvName.substring("pva://".length());
        }
        return pvName;
    }
}
