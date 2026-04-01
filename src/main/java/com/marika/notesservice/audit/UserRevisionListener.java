package com.marika.notesservice.audit;

import org.hibernate.envers.RevisionListener;
import org.springframework.security.core.context.SecurityContextHolder;

public class UserRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        CustomRevisionEntity rev = (CustomRevisionEntity) revisionEntity;

        String username = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        rev.setChangedBy(username);
    }
}
