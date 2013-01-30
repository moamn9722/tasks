/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.api.PermaSql;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.UserActivityDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.User;
import com.todoroo.astrid.data.UserActivity;
import com.todoroo.astrid.tags.TaskToTagMetadata;

/**
 * Service layer for {@link TagData}-centered activities.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TagDataService {

    @Autowired TagDataDao tagDataDao;
    @Autowired TaskDao taskDao;
    @Autowired UserActivityDao userActivityDao;

    public TagDataService() {
        DependencyInjectionService.getInstance().inject(this);
    }

    // --- service layer

    /**
     * Query underlying database
     * @param query
     * @return
     */
    public TodorooCursor<TagData> query(Query query) {
        return tagDataDao.query(query);
    }

    /**
     * Save a single piece of metadata
     * @param metadata
     */
    public boolean save(TagData tagData) {
        return tagDataDao.persist(tagData);
    }

    /**
     * Delete a model
     */
    public void delete(long id) {
        tagDataDao.delete(id);
    }

    /**
     * Delete many
     */
    public void deleteWhere(Criterion where) {
        tagDataDao.deleteWhere(where);
    }

    /**
     *
     * @param properties
     * @param id id
     * @return item, or null if it doesn't exist
     */
    public TagData fetchById(long id, Property<?>... properties) {
        return tagDataDao.fetch(id, properties);
    }

    /**
     * Find a tag by name
     * @return null if doesn't exist
     */
    public TagData getTag(String name, Property<?>... properties) {
        TodorooCursor<TagData> cursor = tagDataDao.query(Query.select(properties).where(TagData.NAME.eqCaseInsensitive(name)));
        try {
            if(cursor.getCount() == 0)
                return null;
            cursor.moveToFirst();
            return new TagData(cursor);
        } finally {
            cursor.close();
        }
    }

    /**
     * Fetch tag data
     * @param queryTemplate
     * @param constraint
     * @param properties
     * @return
     */
    @SuppressWarnings("nls")
    public TodorooCursor<TagData> fetchFiltered(String queryTemplate, CharSequence constraint,
            Property<?>... properties) {
        Criterion whereConstraint = null;
        if(constraint != null)
            whereConstraint = Functions.upper(TagData.NAME).like("%" +
                    constraint.toString().toUpperCase() + "%");

        if(queryTemplate == null) {
            if(whereConstraint == null)
                return tagDataDao.query(Query.select(properties));
            else
                return tagDataDao.query(Query.select(properties).where(whereConstraint));
        }

        String sql;
        if(whereConstraint != null) {
            if(!queryTemplate.toUpperCase().contains("WHERE"))
                sql = queryTemplate + " WHERE " + whereConstraint;
            else
                sql = queryTemplate.replace("WHERE ", "WHERE " + whereConstraint + " AND ");
        } else
            sql = queryTemplate;

        sql = PermaSql.replacePlaceholders(sql);

        return tagDataDao.query(Query.select(properties).withQueryTemplate(sql));
    }

    /**
     * Get updates for this tagData
     * @return
     */
    public TodorooCursor<UserActivity> getUpdates(TagData tagData) {
        return getUpdatesWithExtraCriteria(tagData, Criterion.all);
    }

    private static Query queryForTagData(TagData tagData) {
        return Query.select(AndroidUtilities.addToArray(UserActivity.PROPERTIES, User.PROPERTIES)).where(Criterion.or(
                Criterion.and(UserActivity.ACTION.eq(UserActivity.ACTION_TAG_COMMENT), UserActivity.TARGET_ID.eq(tagData.getUuid())),
                Criterion.and(UserActivity.ACTION.eq(UserActivity.ACTION_TASK_COMMENT),
                        UserActivity.TARGET_ID.in(Query.select(TaskToTagMetadata.TASK_UUID)
                                .from(Metadata.TABLE).where(Criterion.and(MetadataCriteria.withKey(TaskToTagMetadata.KEY), TaskToTagMetadata.TAG_UUID.eq(tagData.getUuid())))))))
                .join(Join.left(User.TABLE, UserActivity.USER_UUID.eq(User.UUID)));
    }

    public TodorooCursor<UserActivity> getUpdatesWithExtraCriteria(TagData tagData, Criterion criterion) {
        if (tagData == null)
            return userActivityDao.query(Query.select(UserActivity.PROPERTIES).where(
                    criterion).
                    orderBy(Order.desc(UserActivity.CREATED_AT)));

        return userActivityDao.query(queryForTagData(tagData).orderBy(Order.desc(UserActivity.CREATED_AT)));
    }

    /**
     * Return update
     * @param tagData
     * @return
     */
    public UserActivity getLatestUpdate(TagData tagData) {
        if(RemoteModel.NO_UUID.equals(tagData.getValue(TagData.UUID)))
            return null;

        TodorooCursor<UserActivity> updates = userActivityDao.query(queryForTagData(tagData).orderBy(Order.desc(UserActivity.CREATED_AT)).limit(1));
        try {
            if(updates.getCount() == 0)
                return null;
            updates.moveToFirst();
            return new UserActivity(updates);
        } finally {
            updates.close();
        }
    }

}
