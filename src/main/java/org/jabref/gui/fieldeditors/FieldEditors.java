package org.jabref.gui.fieldeditors;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.undo.UndoManager;

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.AutoCompleteSuggestionProvider;
import org.jabref.gui.autocompleter.ContentSelectorSuggestionProvider;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.FieldProperty;
import org.jabref.model.entry.InternalBibtexFields;
import org.jabref.model.metadata.MetaData;
import org.jabref.preferences.JabRefPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.model.entry.FieldName.AUTHOR;
import static org.jabref.model.entry.FieldName.INSTITUTION;
import static org.jabref.model.entry.FieldName.TITLE;
import static org.jabref.model.entry.FieldName.YEAR;

public class FieldEditors {

    private static final Logger LOGGER = LoggerFactory.getLogger(FieldEditors.class);

    private static final Set<String> SINGLE_LINE_FIELDS = Collections.unmodifiableSet(new HashSet<>(
            Arrays.asList(TITLE, AUTHOR, YEAR, INSTITUTION)
    ));

    public static FieldEditorFX getForField(final String fieldName,
                                            final TaskExecutor taskExecutor,
                                            final DialogService dialogService,
                                            final JournalAbbreviationLoader journalAbbreviationLoader,
                                            final JournalAbbreviationPreferences journalAbbreviationPreferences,
                                            final JabRefPreferences preferences,
                                            final BibDatabaseContext databaseContext,
                                            final String entryType,
                                            final SuggestionProviders suggestionProviders,
                                            final UndoManager undoManager) {
        final Set<FieldProperty> fieldExtras = InternalBibtexFields.getFieldProperties(fieldName);

        final AutoCompleteSuggestionProvider<?> suggestionProvider = getSuggestionProvider(fieldName, suggestionProviders, databaseContext.getMetaData());

        final FieldCheckers fieldCheckers = new FieldCheckers(
                databaseContext,
                preferences.getFileDirectoryPreferences(),
                journalAbbreviationLoader.getRepository(journalAbbreviationPreferences),
                preferences.getBoolean(JabRefPreferences.ENFORCE_LEGAL_BIBTEX_KEY));

        final boolean hasSingleLine = SINGLE_LINE_FIELDS.contains(fieldName.toLowerCase());

        if (preferences.getTimestampPreferences().getTimestampField().equals(fieldName) || fieldExtras.contains(FieldProperty.DATE)) {
            if (fieldExtras.contains(FieldProperty.ISO_DATE)) {
                return new DateEditor(fieldName, DateTimeFormatter.ofPattern("[uuuu][-MM][-dd]"), suggestionProvider, fieldCheckers);
            } else {
                return new DateEditor(fieldName, DateTimeFormatter.ofPattern(Globals.prefs.getTimestampPreferences().getTimestampFormat()), suggestionProvider, fieldCheckers);
            }
        } else if (fieldExtras.contains(FieldProperty.EXTERNAL)) {
            return new UrlEditor(fieldName, dialogService, suggestionProvider, fieldCheckers, preferences);
        } else if (fieldExtras.contains(FieldProperty.JOURNAL_NAME)) {
            return new JournalEditor(fieldName, journalAbbreviationLoader, preferences, suggestionProvider, fieldCheckers);
        } else if (fieldExtras.contains(FieldProperty.DOI) || fieldExtras.contains(FieldProperty.EPRINT) || fieldExtras.contains(FieldProperty.ISBN)) {
            return new IdentifierEditor(fieldName, taskExecutor, dialogService, suggestionProvider, fieldCheckers, preferences);
        } else if (fieldExtras.contains(FieldProperty.OWNER)) {
            return new OwnerEditor(fieldName, preferences, suggestionProvider, fieldCheckers);
        } else if (fieldExtras.contains(FieldProperty.FILE_EDITOR)) {
            return new LinkedFilesEditor(fieldName, dialogService, databaseContext, taskExecutor, suggestionProvider, fieldCheckers, preferences);
        } else if (fieldExtras.contains(FieldProperty.YES_NO)) {
            return new OptionEditor<>(new YesNoEditorViewModel(fieldName, suggestionProvider, fieldCheckers));
        } else if (fieldExtras.contains(FieldProperty.MONTH)) {
            return new OptionEditor<>(new MonthEditorViewModel(fieldName, suggestionProvider, databaseContext.getMode(), fieldCheckers));
        } else if (fieldExtras.contains(FieldProperty.GENDER)) {
            return new OptionEditor<>(new GenderEditorViewModel(fieldName, suggestionProvider, fieldCheckers));
        } else if (fieldExtras.contains(FieldProperty.EDITOR_TYPE)) {
            return new OptionEditor<>(new EditorTypeEditorViewModel(fieldName, suggestionProvider, fieldCheckers));
        } else if (fieldExtras.contains(FieldProperty.PAGINATION)) {
            return new OptionEditor<>(new PaginationEditorViewModel(fieldName, suggestionProvider, fieldCheckers));
        } else if (fieldExtras.contains(FieldProperty.TYPE)) {
            if ("patent".equalsIgnoreCase(entryType)) {
                return new OptionEditor<>(new PatentTypeEditorViewModel(fieldName, suggestionProvider, fieldCheckers));
            } else {
                return new OptionEditor<>(new TypeEditorViewModel(fieldName, suggestionProvider, fieldCheckers));
            }
        } else if (fieldExtras.contains(FieldProperty.SINGLE_ENTRY_LINK) || fieldExtras.contains(FieldProperty.MULTIPLE_ENTRY_LINK)) {
            return new LinkedEntriesEditor(fieldName, databaseContext, suggestionProvider, fieldCheckers);
        } else if (fieldExtras.contains(FieldProperty.PERSON_NAMES)) {
            return new PersonsEditor(fieldName, suggestionProvider, preferences, fieldCheckers, hasSingleLine);
        } else if (FieldName.KEYWORDS.equals(fieldName)) {
            return new KeywordsEditor(fieldName, suggestionProvider, fieldCheckers, preferences);
        } else if (fieldExtras.contains(FieldProperty.MULTILINE_TEXT)) {
            return new MultilineEditor(fieldName, suggestionProvider, fieldCheckers, preferences);
        } else if (fieldExtras.contains(FieldProperty.KEY)) {
            return new BibtexKeyEditor(fieldName, preferences, suggestionProvider, fieldCheckers, databaseContext, undoManager, dialogService);
        }

        // default
        return new SimpleEditor(fieldName, suggestionProvider, fieldCheckers, preferences, hasSingleLine);
    }

    @SuppressWarnings("unchecked")
    private static AutoCompleteSuggestionProvider<?> getSuggestionProvider(String fieldName, SuggestionProviders suggestionProviders, MetaData metaData) {
        AutoCompleteSuggestionProvider<?> suggestionProvider = suggestionProviders.getForField(fieldName);

        List<String> contentSelectorValues = metaData.getContentSelectorValuesForField(fieldName);
        if (!contentSelectorValues.isEmpty()) {
            // Enrich auto completion by content selector values
            try {
                return new ContentSelectorSuggestionProvider((AutoCompleteSuggestionProvider<String>) suggestionProvider, contentSelectorValues);
            } catch (ClassCastException exception) {
                LOGGER.error("Content selectors are only supported for normal fields with string-based auto completion.");
                return suggestionProvider;
            }
        } else {
            return suggestionProvider;
        }
    }
}
