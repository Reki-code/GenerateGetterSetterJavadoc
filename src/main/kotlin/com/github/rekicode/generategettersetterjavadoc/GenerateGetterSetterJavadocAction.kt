package com.github.rekicode.generategettersetterjavadoc

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.*
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBList
import org.jetbrains.annotations.NonNls
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.ListSelectionModel

class GenerateGetterSetterJavadocAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        // Get the current project, return if null
        val project = e.project ?: return

        // Get the current file
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        // Check if the file is a Java file
        if (psiFile !is PsiJavaFile) return
        // Get the classes in the Java file
        val classes: Array<PsiClass> = psiFile.classes
        // Loop through each class
        for (aClass in classes) {
            // Create a dialog for field selection
            val dialog = FieldSelectionDialog(project, aClass.fields)
            // Show the dialog
            dialog.show()
            // Check if the dialog was confirmed
            if (!dialog.isOK) continue
            // Get the selected fields
            val selectedFields: List<PsiField> = dialog.selectedFields
            // Run a write command action
            writeSetterGetter(project, selectedFields, aClass)
        }
    }

    private fun writeSetterGetter(
        project: Project,
        selectedFields: List<PsiField>,
        aClass: PsiClass
    ) {
        WriteCommandAction.runWriteCommandAction(project) {
            // Loop through each selected field
            for (field in selectedFields) {
                // Generate the getter and setter for the field
                val getter: PsiMethod = generateGetter(project, field)
                val setter: PsiMethod = generateSetter(project, field)
                // Add the getter and setter to the class
                aClass.add(getter)
                aClass.add(setter)
            }
        }
    }

    // Define a method to generate a getter for a field
    private fun generateGetter(project: Project, field: PsiField): PsiMethod {
        // Get the field name
        val fieldName = field.name
        // Generate the method name
        val preffix = if (field.type.equalsToText("boolean")) "is" else "get"
        val methodName = preffix + capitalize(fieldName)
        // Get the element factory
        val elementFactory: PsiElementFactory = JavaPsiFacade.getElementFactory(project)

        // Extract the doc text
        val fieldDocCommentText = extractDocText(field, fieldName)

        // Generate the getter text
        val getterText = """
            /**
             * ${fieldDocCommentText}の取得
             *
             * @return $fieldDocCommentText
             */
            public ${field.type.presentableText} $methodName() {
                return $fieldName;
            }
        """.trimIndent()

        // Create the method from the text
        return elementFactory.createMethodFromText(getterText, field)
    }

    // Define a method to extract the doc text
    private fun extractDocText(
        field: PsiField,
        fieldName: @NonNls String
    ): String {
        // Get the field's doc comment
        val fieldDocComment = field.docComment?.text ?: fieldName
        // Remove `/**` and `*/` from the doc comment text
        val fieldDocCommentText = fieldDocComment
            .replace("/**", "")
            .replace("*/", "")
            .trim()
        return fieldDocCommentText
    }

    // Define a method to generate a setter for a field
    private fun generateSetter(project: Project, field: PsiField): PsiMethod {
        // Get the field name
        val fieldName = field.name
        // Generate the method name
        val methodName = "set" + capitalize(fieldName)
        // Get the element factory
        val elementFactory: PsiElementFactory = JavaPsiFacade.getElementFactory(project)

        // Extract the doc text
        val fieldDocCommentText = extractDocText(field, fieldName)

        // Generate the setter text
        val setterText = """
                /**
                * ${fieldDocCommentText}の設定
                */
                public void $methodName(${field.type.presentableText} $fieldName) {
                    this.$fieldName = $fieldName;
                }
        """.trimIndent()

        // Create the method from the text
        return elementFactory.createMethodFromText(setterText, field)
    }

    // Define a method to capitalize a string
    private fun capitalize(s: String): String {
        return Character.toUpperCase(s[0]) + s.substring(1)
    }

    // Define a dialog for field selection
    private class FieldSelectionDialog(
        project: Project,
        fields: Array<PsiField>
    ) : DialogWrapper(project) {

        // Define the fields
        private val fields = fields.toList()
        // Define the field list
        private lateinit var fieldList: JBList<PsiField>
        // Define the selected fields
        var selectedFields: List<PsiField> = emptyList()
            private set

        // Initialize the dialog
        init {
            init()
        }

        // Define the center panel of the dialog
        override fun createCenterPanel(): JComponent {
            // Create a model for the list
            val model = DefaultListModel<PsiField>()
            // Add each field to the model
            fields.forEach { model.addElement(it) }
            // Create the list
            fieldList = JBList(model)
            // Set the selection mode of the list
            fieldList.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
            // Set the title of the dialog
            title = "Select Fields"
            // Return a scroll pane containing the list
            return ScrollPaneFactory.createScrollPane(fieldList)
        }

        // Define what happens when the OK button is pressed
        override fun doOKAction() {
            // Set the selected fields
            selectedFields = fieldList.selectedValuesList
            // Call the superclass's method
            super.doOKAction()
        }
    }
}