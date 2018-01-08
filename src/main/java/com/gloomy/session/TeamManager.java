package com.gloomy.session;

import org.pmw.tinylog.Logger;

import java.util.ArrayList;
import java.util.List;

public class TeamManager
{
    private List<Client> sessionClientList;
    private int amountOfTeams;
    private int maxPlayers;
    private final List<Team> teams = new ArrayList<>();

    private TeamManager() {}

    /**
     * Constructs a new TeamManager with a handle on a specified client list and with max session total of players and
     * a specified amount of teams
     * @param clientList Handle to the list of clients to work with (NEEDED?)
     * @param maxPlayers The maximum amount of players allowed in the session
     * @param amountOfTeams The amount of teams that should be created for this manager to manage
     */
    public TeamManager(List<Client> clientList, int maxPlayers, int amountOfTeams)
    {
        this.sessionClientList = clientList;
        this.amountOfTeams     = amountOfTeams;
        this.maxPlayers        = maxPlayers;
    }

    /**
     * Adds a client after finding the team with the least players, and returns the Team that the client is added to.
     * @param client The client that should be added to a team.
     * @return The team that the client is added to
     */
    public Team addClientGetTeam(Client client)
    {
        Team clientsTeam = findTeamWithLeastPlayers();
        return clientsTeam;
    }

    private Team findTeamWithLeastPlayers()
    {
        Team minTeam = getTeam(0);

        for (Team team : teams)
        {
            if (team.teamMembers.size() < minTeam.teamMembers.size())
                minTeam = team;
        }

        return minTeam; // Just return the first team if all teams have the same amount of players.
    }

    /**
     * Returns the team with the specified id
     * @param id The id to find the team of
     * @return The team with the id specified
     */
    private Team getTeamFromId(int id)
    {
        for (Team team : teams)
            if (team.id == id)
                return team;

        Logger.warn("No team with id {} was found, returning null.", id);
        return null;
    }

    private Team getTeam(int team) { return teams.get(team); }

    /**
     * Team class is a container for clients and team id
     */
    class Team
    {
        public int id = 0;
        public List<Client> teamMembers;

        public Team(int id, int teamSize)
        {
            this.id = id;
            teamMembers = new ArrayList<>();

            for (int i = 0; i < teamSize; i++)
                teamMembers.add(null);
        }
    }
}
