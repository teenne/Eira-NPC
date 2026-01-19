# Contributing to Storyteller NPCs

Thank you for your interest in contributing to Storyteller NPCs! This document provides guidelines and information for contributors.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [How to Contribute](#how-to-contribute)
- [Development Workflow](#development-workflow)
- [Pull Request Process](#pull-request-process)
- [Style Guidelines](#style-guidelines)
- [Community](#community)

---

## Code of Conduct

### Our Standards

- Be respectful and inclusive
- Welcome newcomers and help them learn
- Accept constructive criticism gracefully
- Focus on what's best for the community
- Show empathy towards others

### Unacceptable Behavior

- Harassment, trolling, or personal attacks
- Discriminatory language or imagery
- Publishing others' private information
- Other conduct deemed inappropriate

---

## Getting Started

### Prerequisites

1. Read the [Development Setup Guide](docs/DEVELOPMENT.md)
2. Set up your development environment
3. Familiarize yourself with the [Architecture](docs/ARCHITECTURE.md)

### Finding Things to Work On

- **Good First Issues**: Labeled `good-first-issue`
- **Help Wanted**: Labeled `help-wanted`
- **Bugs**: Labeled `bug`
- **Features**: Labeled `enhancement`

Check the [issue tracker](https://github.com/yourusername/storyteller/issues) for open issues.

---

## How to Contribute

### Reporting Bugs

Before creating a bug report:
1. Check existing issues for duplicates
2. Test with the latest version
3. Gather relevant information

**Include in your report:**
- Minecraft version
- Mod version
- NeoForge version
- Steps to reproduce
- Expected vs actual behavior
- Logs (if applicable)
- Screenshots (if applicable)

### Suggesting Features

We welcome feature suggestions! Please:
1. Check existing suggestions for duplicates
2. Describe the use case clearly
3. Explain why this benefits users
4. Consider implementation complexity

### Contributing Code

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Write/update tests
5. Update documentation
6. Submit a pull request

### Contributing Documentation

Documentation improvements are always welcome:
- Fix typos and grammar
- Clarify confusing sections
- Add examples
- Translate to other languages

### Contributing Characters

Share your character creations:
1. Create a well-designed character JSON
2. Test thoroughly
3. Submit as a PR to `examples/community/`
4. Include a brief description

---

## Development Workflow

### Branch Naming

```
feature/short-description    # New features
fix/issue-number-description # Bug fixes
docs/what-changed           # Documentation
refactor/what-changed       # Code refactoring
```

### Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
type(scope): brief description

Longer explanation if needed.
Explain the problem, solution, and why.

Fixes #123
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation only
- `style`: Code style (formatting, etc.)
- `refactor`: Code refactoring
- `test`: Adding tests
- `chore`: Build process, dependencies

**Examples:**
```
feat(llm): add support for local Llama models

fix(chat): prevent crash when NPC is unloaded during conversation

docs(readme): add troubleshooting section for Ollama setup
```

### Testing

Before submitting:

- [ ] Code compiles without errors
- [ ] Manual testing completed
- [ ] No regressions in existing features
- [ ] New features have tests (where applicable)

---

## Pull Request Process

### Before Submitting

1. **Update from main**: Rebase your branch
2. **Test thoroughly**: Manual and automated
3. **Update docs**: If changing behavior
4. **Update CHANGELOG**: Add your changes

### PR Description Template

```markdown
## Description
Brief description of changes.

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Documentation
- [ ] Refactoring

## Testing Done
- [ ] Tested in singleplayer
- [ ] Tested in multiplayer
- [ ] Tested with different LLM providers

## Screenshots (if applicable)

## Related Issues
Fixes #123
```

### Review Process

1. Maintainer reviews within 1 week
2. Address feedback promptly
3. Re-request review after changes
4. Squash commits before merge (usually)

### After Merge

- Delete your feature branch
- Update your fork
- Celebrate! 🎉

---

## Style Guidelines

### Java Code Style

```java
// Classes: PascalCase
public class StorytellerNPC { }

// Methods/variables: camelCase
public void processMessage(String message) { }
private int conversationCount;

// Constants: UPPER_SNAKE_CASE
public static final int MAX_HISTORY = 20;

// Braces: Same line
if (condition) {
    // code
} else {
    // code
}

// Line length: 120 characters max
// Indentation: 4 spaces (no tabs)
```

### JSON Style

```json
{
  "id": "example",
  "name": "Example",
  "nested": {
    "property": "value"
  },
  "array": [
    "item1",
    "item2"
  ]
}
```

### Documentation Style

- Use Markdown
- Include code examples
- Keep language clear and simple
- Use headers for organization
- Include links to related docs

---

## Community

### Getting Help

- **Discord**: [Join our server](#)
- **GitHub Discussions**: Ask questions
- **Issues**: Bug reports and features

### Recognition

Contributors are recognized in:
- CHANGELOG.md
- README.md credits section
- GitHub contributors page

### Maintainers

- [@yourusername](https://github.com/yourusername) - Lead maintainer

---

## License

By contributing, you agree that your contributions will be licensed under the MIT License.

---

Thank you for contributing to Storyteller NPCs! 🎭
