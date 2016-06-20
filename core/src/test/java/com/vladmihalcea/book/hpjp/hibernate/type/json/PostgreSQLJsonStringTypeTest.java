package com.vladmihalcea.book.hpjp.hibernate.type.json;

import com.vladmihalcea.book.hpjp.hibernate.type.json.model.BaseEntity;
import com.vladmihalcea.book.hpjp.hibernate.type.json.model.Location;
import com.vladmihalcea.book.hpjp.hibernate.type.json.model.Ticket;
import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.annotations.Type;
import org.junit.Test;

import javax.persistence.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLJsonStringTypeTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Event.class,
            Participant.class
        };
    }

    @Test
    public void test() {
        final AtomicReference<Event> eventHolder = new AtomicReference<>();
        final AtomicReference<Participant> participantHolder = new AtomicReference<>();

        doInJPA(entityManager -> {
            entityManager.persist(new Event());

            Location location = new Location();
            location.setCountry("Romania");
            location.setCity("Cluj-Napoca");
            Event event = new Event();
            event.setValue(location);

            Ticket ticket = new Ticket();
            ticket.setPrice(12.34d);
            ticket.setRegistrationCode("ABC123");
            Participant participant = new Participant();
            participant.setTicket(ticket);

            entityManager.persist(event);
            entityManager.persist(participant);

            eventHolder.set(event);
            participantHolder.set(participant);
        });
        doInJPA(entityManager -> {
            Event event = entityManager.find(Event.class, eventHolder.get().getId());
            assertEquals("Cluj-Napoca", event.getLocation().getCity());

            Participant participant = entityManager.find(Participant.class, participantHolder.get().getId());
            assertEquals("ABC123", participant.getTicket().getRegistrationCode());

            List<Object> participants = entityManager.createNativeQuery(
                "select p.ticket ->>'registrationCode' " +
                "from participant p " +
                "where p.ticket ->> 'price' > '10'")
            .getResultList();

            event.getLocation().setCity("Constanta");
            entityManager.flush();

            assertEquals(1, participants.size());
        });
    }

    @Entity(name = "Event")
    @Table(name = "event")
    public static class Event extends BaseEntity {

        @Id
        @GeneratedValue
        private Long id;

        @Type(type = "jsonb")
        @Column(columnDefinition = "json")
        protected Location location;

        public Event() {}

        public Long getId() {
            return id;
        }

        public Location getLocation() {
            return location;
        }

        public void setValue(Location location) {
            this.location = location;
        }
    }

    @Entity(name = "Participant")
    @Table(name = "participant")
    public static class Participant extends BaseEntity {

        @Id
        @GeneratedValue
        private Long id;

        @Type(type = "jsonb")
        @Column(columnDefinition = "json")
        protected Ticket ticket;

        public Long getId() {
            return id;
        }

        public Ticket getTicket() {
            return ticket;
        }

        public void setTicket(Ticket ticket) {
            this.ticket = ticket;
        }
    }

}
