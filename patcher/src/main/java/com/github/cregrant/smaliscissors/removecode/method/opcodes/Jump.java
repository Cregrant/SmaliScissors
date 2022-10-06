package com.github.cregrant.smaliscissors.removecode.method.opcodes;

import java.util.List;

public interface Jump {

    Tag getTag();

    List<TableTag> getTableTags();
}
