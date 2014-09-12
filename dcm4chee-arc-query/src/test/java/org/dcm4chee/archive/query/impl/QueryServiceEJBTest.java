/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 * Java(TM), hosted at https://github.com/gunterze/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * Agfa Healthcare.
 * Portions created by the Initial Developer are Copyright (C) 2011-2014
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See @authors listed below
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.dcm4chee.archive.query.impl;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.same;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.dcm4chee.archive.conf.QueryParam;
import org.dcm4chee.archive.entity.QInstance;
import org.dcm4chee.archive.entity.QSeries;
import org.dcm4chee.archive.entity.Series;
import org.dcm4chee.archive.entity.Study;
import org.dcm4chee.mysema.query.jpa.hibernate.DetachedHibernateQueryFactory;
import org.easymock.EasyMockSupport;
import org.hibernate.Session;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.mysema.query.jpa.hibernate.HibernateSubQuery;
import com.mysema.query.types.EntityPath;
import com.mysema.query.types.ExpressionUtils;
import com.mysema.query.types.Path;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.expr.BooleanExpression;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ BooleanBuilder.class, HibernateQuery.class,
        HibernateSubQuery.class, ExpressionUtils.class })
public class QueryServiceEJBTest {
    private static final Long STUDY_PK = 3L;

    private static final int UPDATED_ROW_COUNT = 5;

    private static final int NUMBER_OF_STUDY_RELATED_SERIES = 7;

    private static final int NUMBER_OF_STUDY_RELATED_INSTANCES = 11;

    private static final int NUMBER_OF_SERIES_RELATED_INSTANCES = 13;

    private static final Long SERIES_PK = 17L;

    EasyMockSupport easyMockSupport;

    EntityManager mockEntityManager;

    BooleanBuilder mockBooleanBuilder;

    DetachedHibernateQueryFactory mockDetachedHibernateQueryFactory;

    HibernateQuery mockHibernateQuery;

    QueryServiceEJB cut;

    @Before
    public void before() {
        easyMockSupport = new EasyMockSupport();

        mockEntityManager = easyMockSupport.createMock(EntityManager.class);
        mockBooleanBuilder = PowerMock.createMock(BooleanBuilder.class);
        mockDetachedHibernateQueryFactory = easyMockSupport
                .createMock(DetachedHibernateQueryFactory.class);
        mockHibernateQuery = PowerMock.createMock(HibernateQuery.class);

        cut = easyMockSupport.createMockBuilder(QueryServiceEJB.class)
                .addMockedMethod("createBooleanBuilder").createMock();
        cut.em = mockEntityManager;
        cut.queryFactory = mockDetachedHibernateQueryFactory;
    }

    @Test
    public void calculateNumberOfStudyRelatedSeries_shouldNotUpdateNumberOfSeries_whenNumberOfInstancesCacheSlotIs0() {
        QueryParam mockQueryParam = easyMockSupport
                .createMock(QueryParam.class);

        expect(mockQueryParam.getNumberOfInstancesCacheSlot()).andReturn(0);

        calculateNumbeOfStudyRelatedSeries(mockQueryParam);
    }

    @Test
    public void calculateNumberOfStudyRelatedSeries_shouldNotUpdateNumberOfSeries_whenNumberOfInstancesCacheSlotIsMinus1() {
        QueryParam mockQueryParam = easyMockSupport
                .createMock(QueryParam.class);

        expect(mockQueryParam.getNumberOfInstancesCacheSlot()).andReturn(-1);

        calculateNumbeOfStudyRelatedSeries(mockQueryParam);
    }

    @Test
    public void calculateNumberOfStudyRelatedSeries_shouldUpdateNumberOfSeries1_whenNumberOfInstancesCacheSlotIs1() {
        QueryParam mockQueryParam = easyMockSupport
                .createMock(QueryParam.class);
        Query mockQuery = easyMockSupport.createMock(Query.class);

        expect(mockQueryParam.getNumberOfInstancesCacheSlot()).andReturn(1);
        expect(
                mockEntityManager
                        .createNamedQuery(Study.UPDATE_NUMBER_OF_SERIES[0]))
                .andReturn(mockQuery);

        expect(
                mockQuery.setParameter(eq(1),
                        eq(NUMBER_OF_STUDY_RELATED_SERIES))).andReturn(
                mockQuery);
        expect(mockQuery.setParameter(2, STUDY_PK)).andReturn(mockQuery);
        expect(mockQuery.executeUpdate()).andReturn(UPDATED_ROW_COUNT);

        calculateNumbeOfStudyRelatedSeries(mockQueryParam);
    }

    @Test
    public void calculateNumberOfStudyRelatedInstance_shouldNotUpdateNumberOfInstances_whenNumberOfInstancesCacheSlotIs0() {
        QueryParam mockQueryParam = easyMockSupport
                .createMock(QueryParam.class);

        expect(mockQueryParam.getNumberOfInstancesCacheSlot()).andReturn(0);

        calculateNumberOfStudyRelatedInstance(mockQueryParam);
    }

    @Test
    public void calculateNumberOfStudyRelatedInstance_shouldNotUpdateNumberOfInstances_whenNumberOfInstancesCacheSlotIsMinus1() {
        QueryParam mockQueryParam = easyMockSupport
                .createMock(QueryParam.class);

        expect(mockQueryParam.getNumberOfInstancesCacheSlot()).andReturn(-1);

        calculateNumberOfStudyRelatedInstance(mockQueryParam);
    }

    @Test
    public void calculateNumberOfStudyRelatedInstance_shouldUpdateNumberOfInstances2_whenNumberOfInstancesCacheSlotIs2() {
        QueryParam mockQueryParam = easyMockSupport
                .createMock(QueryParam.class);
        Query mockQuery = easyMockSupport.createMock(Query.class);

        expect(mockQueryParam.getNumberOfInstancesCacheSlot()).andReturn(2);

        expect(
                mockEntityManager
                        .createNamedQuery(Study.UPDATE_NUMBER_OF_INSTANCES[1]))
                .andReturn(mockQuery);

        expect(
                mockQuery.setParameter(eq(1),
                        eq(NUMBER_OF_STUDY_RELATED_INSTANCES))).andReturn(
                mockQuery);
        expect(mockQuery.setParameter(2, STUDY_PK)).andReturn(mockQuery);
        expect(mockQuery.executeUpdate()).andReturn(UPDATED_ROW_COUNT);

        calculateNumberOfStudyRelatedInstance(mockQueryParam);
    }

    @Test
    public void calculateNumberOfSeriesRelatedInstance_shouldNotUpdateNumberOfInstances_whenNumberOfInstancesCacheSlotIs0() {
        QueryParam mockQueryParam = easyMockSupport
                .createMock(QueryParam.class);

        expect(mockQueryParam.getNumberOfInstancesCacheSlot()).andReturn(0);

        calculateNumberOfSeriesRelatedInstance(mockQueryParam);
    }

    @Test
    public void calculateNumberOfSeriesRelatedInstance_shouldNotUpdateNumberOfInstances_whenNumberOfInstancesCacheSlotIsMinus1() {
        QueryParam mockQueryParam = easyMockSupport
                .createMock(QueryParam.class);

        expect(mockQueryParam.getNumberOfInstancesCacheSlot()).andReturn(-1);

        calculateNumberOfSeriesRelatedInstance(mockQueryParam);
    }

    @Test
    public void calculateNumberOfSeriesRelatedInstance_shouldNotUpdateNumberOfInstances3_whenNumberOfInstancesCacheSlotIs3() {
        QueryParam mockQueryParam = easyMockSupport
                .createMock(QueryParam.class);
        Query mockQuery = easyMockSupport.createMock(Query.class);

        expect(mockQueryParam.getNumberOfInstancesCacheSlot()).andReturn(3);
        expect(
                mockEntityManager
                        .createNamedQuery(Series.UPDATE_NUMBER_OF_INSTANCES[2]))
                .andReturn(mockQuery);

        expect(
                mockQuery.setParameter(eq(1),
                        eq(NUMBER_OF_SERIES_RELATED_INSTANCES))).andReturn(
                mockQuery);
        expect(mockQuery.setParameter(2, SERIES_PK)).andReturn(mockQuery);
        expect(mockQuery.executeUpdate()).andReturn(UPDATED_ROW_COUNT);

        calculateNumberOfSeriesRelatedInstance(mockQueryParam);
    }

    void calculateNumbeOfStudyRelatedSeries(QueryParam mockQueryParam) {
        HibernateSubQuery mockHibernateSubQuery = PowerMock
                .createMock(HibernateSubQuery.class);
        Predicate mockPredicate = easyMockSupport
                .createNiceMock(Predicate.class);
        BooleanExpression mockBooleanExpression = easyMockSupport
                .createNiceMock(BooleanExpression.class);
        Session mockSession = easyMockSupport.createNiceMock(Session.class);

        PowerMock.mockStatic(ExpressionUtils.class);

        expect(
                cut.createBooleanBuilder(isA(BooleanExpression.class),
                        same(mockQueryParam))).andReturn(mockBooleanBuilder);

        expect(mockEntityManager.unwrap(Session.class)).andReturn(
                mockSession);

        expect(mockDetachedHibernateQueryFactory.query(mockSession)).andReturn(
                mockHibernateQuery);
        expect(mockHibernateQuery.from(QSeries.series)).andReturn(
                mockHibernateQuery);
        expect(mockHibernateQuery.where(mockPredicate)).andReturn(
                mockHibernateQuery);
        expect(mockHibernateQuery.count()).andReturn(
                (long) NUMBER_OF_STUDY_RELATED_SERIES);

        expect(mockDetachedHibernateQueryFactory.subQuery()).andReturn(
                mockHibernateSubQuery);
        expect(mockHibernateSubQuery.from(QInstance.instance)).andReturn(
                mockHibernateSubQuery);
        expect(mockHibernateSubQuery.where(mockBooleanBuilder)).andReturn(
                mockHibernateSubQuery);
        expect(mockHibernateSubQuery.exists()).andReturn(mockBooleanExpression);

        expect(
                ExpressionUtils.and(isA(Predicate.class),
                        same(mockBooleanExpression))).andReturn(mockPredicate);

        easyMockSupport.replayAll();
        PowerMock.replayAll();

        assertThat(cut.calculateNumberOfStudyRelatedSeries(STUDY_PK,
                mockQueryParam), is(NUMBER_OF_STUDY_RELATED_SERIES));

        PowerMock.verifyAll();
        easyMockSupport.verifyAll();
    }

    @SuppressWarnings("unchecked")
    void calculateNumberOfStudyRelatedInstance(QueryParam mockQueryParam) {
        Session mockSession = easyMockSupport.createNiceMock(Session.class);

        expect(
                cut.createBooleanBuilder(isA(BooleanExpression.class),
                        same(mockQueryParam))).andReturn(mockBooleanBuilder);

        expect(mockEntityManager.unwrap(Session.class)).andReturn(
                mockSession);

        expect(mockDetachedHibernateQueryFactory.query(mockSession)).andReturn(
                mockHibernateQuery);
        expect(mockHibernateQuery.from(QInstance.instance)).andReturn(
                mockHibernateQuery);
        expect(
                mockHibernateQuery.innerJoin(isA(EntityPath.class),
                        isA(Path.class))).andReturn(mockHibernateQuery);
        expect(mockHibernateQuery.where(mockBooleanBuilder)).andReturn(
                mockHibernateQuery);
        expect(mockHibernateQuery.count()).andReturn(
                (long) NUMBER_OF_STUDY_RELATED_INSTANCES);

        easyMockSupport.replayAll();
        PowerMock.replayAll();

        assertThat(cut.calculateNumberOfStudyRelatedInstance(STUDY_PK,
                mockQueryParam), is(NUMBER_OF_STUDY_RELATED_INSTANCES));

        PowerMock.verifyAll();
        easyMockSupport.verifyAll();
    }

    void calculateNumberOfSeriesRelatedInstance(QueryParam mockQueryParam) {
        Session mockSession = easyMockSupport.createNiceMock(Session.class);

        expect(
                cut.createBooleanBuilder(isA(BooleanExpression.class),
                        same(mockQueryParam))).andReturn(mockBooleanBuilder);

        expect(mockEntityManager.unwrap(Session.class)).andReturn(
                mockSession);

        expect(mockDetachedHibernateQueryFactory.query(mockSession)).andReturn(
                mockHibernateQuery);
        expect(mockHibernateQuery.from(QInstance.instance)).andReturn(
                mockHibernateQuery);
        expect(mockHibernateQuery.where(mockBooleanBuilder)).andReturn(
                mockHibernateQuery);
        expect(mockHibernateQuery.count()).andReturn(
                (long) NUMBER_OF_SERIES_RELATED_INSTANCES);

        easyMockSupport.replayAll();
        PowerMock.replayAll();

        assertThat(cut.calculateNumberOfSeriesRelatedInstance(SERIES_PK,
                mockQueryParam), is(NUMBER_OF_SERIES_RELATED_INSTANCES));

        PowerMock.verifyAll();
        easyMockSupport.verifyAll();
    }
}