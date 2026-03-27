package com.marika.notesservice.audit;

import jakarta.persistence.Entity;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionEntity;

@Entity
@RevisionEntity(UserRevisionListener.class)
public class CustomRevisionEntity extends DefaultRevisionEntity {

    private String changedBy;

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }
}

