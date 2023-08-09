package com.hari.MedicalPlan.service;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
public class IndexingMessage implements Serializable {
    String action;
    String body;
}
