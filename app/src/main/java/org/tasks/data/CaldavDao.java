package org.tasks.data;

import static org.tasks.db.DbUtils.collect;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import io.reactivex.Single;
import java.util.List;
import org.tasks.filters.CaldavFilters;

@Dao
public abstract class CaldavDao {

  @Query("SELECT * FROM caldav_lists")
  public abstract LiveData<List<CaldavCalendar>> subscribeToCalendars();

  @Query("SELECT * FROM caldav_lists WHERE cdl_uuid = :uuid LIMIT 1")
  public abstract CaldavCalendar getCalendarByUuid(String uuid);

  @Query("SELECT * FROM caldav_accounts WHERE cda_uuid = :uuid LIMIT 1")
  public abstract CaldavAccount getAccountByUuid(String uuid);

  @Query("SELECT COUNT(*) FROM caldav_accounts")
  public abstract Single<Integer> accountCount();

  @Query("SELECT * FROM caldav_accounts ORDER BY UPPER(cda_name) ASC")
  public abstract List<CaldavAccount> getAccounts();

  @Query("UPDATE caldav_accounts SET cda_collapsed = :collapsed WHERE cda_id = :id")
  public abstract void setCollapsed(long id, boolean collapsed);

  @Insert
  public abstract long insert(CaldavAccount caldavAccount);

  @Update
  public abstract void update(CaldavAccount caldavAccount);

  public void insert(CaldavCalendar caldavCalendar) {
    caldavCalendar.setId(insertInternal(caldavCalendar));
  }

  @Insert
  abstract long insertInternal(CaldavCalendar caldavCalendar);

  @Update
  public abstract void update(CaldavCalendar caldavCalendar);

  @Insert
  public abstract long insert(CaldavTask caldavTask);

  @Insert
  public abstract void insert(Iterable<CaldavTask> tasks);

  @Update
  public abstract void update(CaldavTask caldavTask);

  public void update(SubsetCaldav caldavTask) {
    update(caldavTask.getId(), caldavTask.getRemoteParent());
  }

  @Query(
      "UPDATE caldav_tasks SET cd_remote_parent = :remoteParent WHERE cd_id = :id")
  abstract void update(long id,String remoteParent);

  @Update
  public abstract void update(Iterable<CaldavTask> tasks);

  @Delete
  public abstract void delete(CaldavTask caldavTask);

  @Query("SELECT * FROM caldav_tasks WHERE cd_deleted > 0 AND cd_calendar = :calendar")
  public abstract List<CaldavTask> getDeleted(String calendar);

  @Query("UPDATE caldav_tasks SET cd_deleted = :now WHERE cd_task IN (:tasks)")
  public abstract void markDeleted(long now, List<Long> tasks);

  @Query("SELECT * FROM caldav_tasks WHERE cd_task = :taskId AND cd_deleted = 0 LIMIT 1")
  public abstract CaldavTask getTask(long taskId);

  @Query("SELECT cd_remote_id FROM caldav_tasks WHERE cd_task = :taskId AND cd_deleted = 0")
  public abstract String getRemoteIdForTask(long taskId);

  @Query("SELECT * FROM caldav_tasks WHERE cd_calendar = :calendar AND cd_object = :object LIMIT 1")
  public abstract CaldavTask getTask(String calendar, String object);

  @Query("SELECT * FROM caldav_tasks WHERE cd_calendar = :calendar AND cd_remote_id = :remoteId")
  public abstract CaldavTask getTaskByRemoteId(String calendar, String remoteId);

  @Query("SELECT * FROM caldav_tasks WHERE cd_task = :taskId")
  public abstract List<CaldavTask> getTasks(long taskId);

  @Query("SELECT * FROM caldav_tasks WHERE cd_task in (:taskIds) AND cd_deleted = 0")
  public abstract List<CaldavTask> getTasks(List<Long> taskIds);

  @Query(
      "SELECT task.*, caldav_task.* FROM tasks AS task "
          + "INNER JOIN caldav_tasks AS caldav_task ON _id = cd_task "
          + "WHERE cd_deleted = 0 AND cd_vtodo IS NOT NULL AND cd_vtodo != ''")
  public abstract List<CaldavTaskContainer> getTasks();

  @Query(
      "SELECT task.*, caldav_task.* FROM tasks AS task "
          + "INNER JOIN caldav_tasks AS caldav_task ON _id = cd_task "
          + "WHERE cd_calendar = :calendar "
          + "AND modified > cd_last_sync "
          + "AND cd_deleted = 0")
  public abstract List<CaldavTaskContainer> getCaldavTasksToPush(String calendar);

  @Query("SELECT * FROM caldav_lists ORDER BY cdl_name COLLATE NOCASE")
  public abstract List<CaldavCalendar> getCalendars();

  @Query("SELECT * FROM caldav_lists WHERE cdl_uuid = :uuid LIMIT 1")
  public abstract CaldavCalendar getCalendar(String uuid);

  @Query("SELECT cd_object FROM caldav_tasks WHERE cd_calendar = :calendar")
  public abstract List<String> getObjects(String calendar);

  public List<Long> getTasks(String calendar, List<String> objects) {
    return collect(objects, b -> getTasksInternal(calendar, b));
  }

  @Query("SELECT cd_task FROM caldav_tasks WHERE cd_calendar = :calendar AND cd_object IN (:objects)")
  abstract List<Long> getTasksInternal(String calendar, List<String> objects);

  @Query("SELECT * FROM caldav_lists WHERE cdl_account = :account AND cdl_url NOT IN (:urls)")
  public abstract List<CaldavCalendar> findDeletedCalendars(String account, List<String> urls);

  @Query("SELECT * FROM caldav_lists WHERE cdl_account = :account AND cdl_url = :url LIMIT 1")
  public abstract CaldavCalendar getCalendarByUrl(String account, String url);

  @Query("SELECT * FROM caldav_accounts WHERE cda_name = :name COLLATE NOCASE LIMIT 1")
  public abstract CaldavAccount getAccountByName(String name);

  @Query(
      "SELECT caldav_accounts.* from caldav_accounts"
          + " INNER JOIN caldav_tasks ON cd_task = :task"
          + " INNER JOIN caldav_lists ON cd_calendar = cdl_uuid"
          + " WHERE cdl_account = cda_uuid")
  public abstract CaldavAccount getAccountForTask(long task);

  @Query("SELECT DISTINCT cd_calendar FROM caldav_tasks WHERE cd_deleted = 0 AND cd_task IN (:tasks)")
  public abstract List<String> getCalendars(List<Long> tasks);

  @Query(
      "SELECT caldav_lists.*, COUNT(tasks._id) AS count"
          + " FROM caldav_lists"
          + " LEFT JOIN caldav_tasks ON caldav_tasks.cd_calendar = caldav_lists.cdl_uuid"
          + " LEFT JOIN tasks ON caldav_tasks.cd_task = tasks._id AND tasks.deleted = 0 AND tasks.completed = 0 AND tasks.hideUntil < :now AND cd_deleted = 0"
          + " WHERE caldav_lists.cdl_account = :uuid"
          + " GROUP BY caldav_lists.cdl_uuid")
  public abstract List<CaldavFilters> getCaldavFilters(String uuid, long now);

  @Query(
      "SELECT tasks._id FROM tasks "
          + "INNER JOIN tags ON tags.task = tasks._id "
          + "INNER JOIN caldav_tasks ON cd_task = tasks._id "
          + "GROUP BY tasks._id")
  public abstract List<Long> getTasksWithTags();

  @Query(
      "UPDATE tasks SET parent = IFNULL(("
          + " SELECT p.cd_task FROM caldav_tasks AS p"
          + "  INNER JOIN caldav_tasks ON caldav_tasks.cd_task = tasks._id"
          + "  WHERE p.cd_remote_id = caldav_tasks.cd_remote_parent"
          + "    AND p.cd_calendar = caldav_tasks.cd_calendar"
          + "    AND p.cd_deleted = 0), 0)"
          + "WHERE _id IN (SELECT _id FROM tasks INNER JOIN caldav_tasks ON _id = cd_task WHERE cd_deleted = 0)")
  public abstract void updateParents();

  @Query(
      "UPDATE tasks SET parent = IFNULL(("
          + " SELECT p.cd_task FROM caldav_tasks AS p"
          + "  INNER JOIN caldav_tasks "
          + "    ON caldav_tasks.cd_task = tasks._id"
          + "    AND caldav_tasks.cd_calendar = :calendar"
          + "  WHERE p.cd_remote_id = caldav_tasks.cd_remote_parent"
          + "    AND p.cd_calendar = caldav_tasks.cd_calendar"
          + "    AND caldav_tasks.cd_deleted = 0), 0)"
          + "WHERE _id IN (SELECT _id FROM tasks INNER JOIN caldav_tasks ON _id = cd_task WHERE cd_deleted = 0 AND cd_calendar = :calendar)")
  public abstract void updateParents(String calendar);
}
